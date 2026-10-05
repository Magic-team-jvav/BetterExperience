package com.github.edg_thexu.better_experience.client;

import com.github.edg_thexu.better_experience.item.MagicBoomStaff;
import com.github.edg_thexu.better_experience.config.ClientConfig;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.networks.c2s.StaffSelectionPacketC2S;
import java.util.Optional;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionDirection;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffAimRange;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffAimCursor;
import com.github.edg_thexu.better_experience.networks.c2s.BreakBlocksPacketC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-owned selection; never stored on the singleton Item instance. */
public final class StaffSelectionHandler {
    private static ItemStack selectedStack = ItemStack.EMPTY;
    private static int selectedSlot = -1;
    private static Object selectedLevel;
    private static BlockPos posA;
    private static BlockPos posB;
    private static BlockPos lastValidPoint;
    private static final StaffAimCursor LOCKED_CURSOR = new StaffAimCursor();
    private static StaffSelectionBounds displayedLockedBounds;
    private static StaffSelectionShape displayedLockedShape;
    private static long lastLockedMovementMillis = -1;
    public static final long HIGHLIGHT_SETTLE_MILLIS = 180;
    private static boolean selecting;

    private static MagicBoomStaff activeStaff() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            reset();
            selectedStack = ItemStack.EMPTY;
            selectedSlot = -1;
            selectedLevel = null;
            return null;
        }
        ItemStack stack = mc.player.getMainHandItem();
        int slot = mc.player.getInventory().selected;
        // Inventory synchronization can replace an ItemStack without switching staffs.
        if (selectedSlot != slot || selectedStack.getItem() != stack.getItem() || selectedLevel != mc.level) {
            reset();
            selectedLevel = mc.level;
        }
        selectedStack = stack;
        selectedSlot = slot;
        return stack.getItem() instanceof MagicBoomStaff staff ? staff : null;
    }

    public static void tick() {
        activeStaff();
    }

    private static void reset() {
        posA = null;
        posB = null;
        lastValidPoint = null;
        LOCKED_CURSOR.reset();
        displayedLockedBounds = null;
        displayedLockedShape = null;
        lastLockedMovementMillis = -1;
        selecting = false;
    }

    public static boolean mouseClicked(int button) {
        MagicBoomStaff staff = activeStaff();
        Minecraft mc = Minecraft.getInstance();
        if (staff == null || mc.screen != null) return false;
        StaffSelectionShape shape = selectedStack.get(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get());
        if (button == 2 && shape != null && Screen.hasShiftDown()) {
            moveLockedDistance(staff, 1);
            return true;
        }
        if (button != 0 && button != 1) return false;
        if (button == 1) {
            if (shape != null) {
                saveShape(null);
                reset();
                message("reselect");
            } else if (selecting && posB != null) {
                saveShape(StaffSelectionShape.fromCorners(posA, posB));
                selecting = false;
                lastValidPoint = posA;
                LOCKED_CURSOR.reset();
                message("locked");
            } else if (selecting) {
                reset();
                message("reselect");
            } else message("choose_corners");
            return true;
        }
        if (shape != null) {
            // Use the last rendered block-aligned bounds rather than aiming again on click.
            StaffSelectionBounds bounds = ClientConfig.SHOW_OUTLINES.get() && shape.equals(displayedLockedShape)
                    ? displayedLockedBounds : null;
            if (bounds == null) {
                Vec3 anchor = lockedTarget(staff);
                if (anchor != null) bounds = shape.centeredAt(anchor);
            }
            if (bounds == null) message("no_target");
            else if (!mc.player.getCooldowns().isOnCooldown(staff))
                PacketDistributor.sendToServer(new BreakBlocksPacketC2S(bounds.center(), mc.player.getInventory().selected));
            return true;
        }
        if (!selecting) {
            if (!StaffKeyMappings.SELECT_MODIFIER.isDown()) {
                mc.player.displayClientMessage(Component.translatable("better_experience.staff.enter_selection",
                        StaffKeyMappings.SELECT_MODIFIER.getTranslatedKeyMessage()), true);
                return true;
            }
            selecting = true;
        }
        if (posB != null) return true;
        BlockPos target = updateTarget(staff);
        if (target == null) {
            message("no_target");
        } else if (posA == null) {
            posA = target;
            message("first_corner");
        } else if (!StaffSelectionBounds.of(posA, target).fits(staff.maxRange * 2 + 1)) {
            message("too_large");
        } else if (!StaffSelectionBounds.of(posA, target).withinReach(mc.player.getEyePosition(), staff.maxRange * 2 + 1)) {
            message("out_of_reach");
        } else {
            posB = target;
            showSize();
        }
        return true;
    }

    public static boolean scroll(double delta) {
        MagicBoomStaff staff = activeStaff();
        Minecraft mc = Minecraft.getInstance();
        if (staff == null || mc.screen != null) return false;
        StaffSelectionShape shape = selectedStack.get(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get());
        if (shape != null) {
            if (!Screen.hasShiftDown()) return false;
            if (delta != 0) moveLockedDistance(staff, delta > 0 ? 1 : -1);
            return true;
        }
        if (!selecting) return false;
        if (posB != null && delta != 0) {
            adjustBoundary(StaffSelectionDirection.fromRotation(mc.player.getYRot(), mc.player.getXRot()),
                    delta > 0 ? 1 : -1, staff.maxRange * 2 + 1);
            showSize();
        }
        return true;
    }

    /** Expand/contract the forward-facing boundaries; diagonals adjust two axes together. */
    private static void adjustBoundary(StaffSelectionDirection direction, int delta, int maxSide) {
        StaffSelectionBounds bounds = StaffSelectionBounds.of(posA, posB);
        StaffSelectionBounds adjusted = bounds.adjust(direction, delta, maxSide);
        if (adjusted == null) return;
        Minecraft mc = Minecraft.getInstance();
        BlockPos nextMin = adjusted.min();
        BlockPos nextMax = adjusted.max();
        double reach = ((MagicBoomStaff) selectedStack.getItem()).maxRange * 2 + 1;
        if (nextMin.getY() < mc.level.getMinBuildHeight() || nextMax.getY() >= mc.level.getMaxBuildHeight()
                || !adjusted.withinReach(mc.player.getEyePosition(), reach)) return;
        posA = nextMin;
        posB = nextMax;
    }

    public static StaffSelectionBounds preview() {
        MagicBoomStaff staff = activeStaff();
        if (staff == null) return null;
        StaffSelectionShape shape = selectedStack.get(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get());
        if (shape != null) {
            Vec3 anchor = lockedTarget(staff);
            displayedLockedBounds = anchor == null ? null : shape.centeredAt(anchor);
            displayedLockedShape = shape;
            return displayedLockedBounds;
        }
        if (!selecting) return null;
        if (posB != null) return StaffSelectionBounds.of(posA, posB);
        BlockPos target = updateTarget(staff);
        if (posA == null) return target == null ? null : StaffSelectionBounds.of(target, target);
        StaffSelectionBounds bounds = StaffSelectionBounds.of(posA, target == null ? posA : target);
        return bounds;
    }

    public static boolean isLocked() {
        return activeStaff() != null && selectedStack.has(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get());
    }

    private static void saveShape(StaffSelectionShape shape) {
        if (shape == null) selectedStack.remove(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get());
        else selectedStack.set(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get(), shape);
        PacketDistributor.sendToServer(new StaffSelectionPacketC2S(
                Minecraft.getInstance().player.getInventory().selected, Optional.ofNullable(shape)));
    }

    private static BlockPos target(MagicBoomStaff staff) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 from = mc.player.getEyePosition();
        Vec3 to = from.add(mc.player.getLookAngle().scale(staff.maxRange * 2));
        var hit = mc.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }

    /** Freeze the displayed endpoint outside the allowed range; clicks use that same endpoint. */
    private static BlockPos updateTarget(MagicBoomStaff staff) {
        BlockPos candidate = target(staff);
        if (candidate != null) {
            StaffSelectionBounds bounds = StaffSelectionBounds.of(posA == null ? candidate : posA, candidate);
            if (allowed(bounds, staff)) {
                lastValidPoint = candidate.immutable();
            }
        }
        return lastValidPoint;
    }

    private static StaffAimRange aimRange(MagicBoomStaff staff) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 eye = mc.player.getEyePosition();
        double reach = staff.maxRange * 2;
        var hit = mc.level.clip(new ClipContext(eye, eye.add(mc.player.getLookAngle().scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        double blockDistance = hit.getType() == HitResult.Type.BLOCK
                ? eye.distanceTo(hit.getLocation()) : Double.NaN;
        return StaffAimRange.of(blockDistance, reach);
    }

    private static Vec3 lockedTarget(MagicBoomStaff staff) {
        Minecraft mc = Minecraft.getInstance();
        StaffAimRange range = aimRange(staff);
        // Without a solid target, start at maximum reach and still preview an air block.
        double initialDistance = target(staff) == null ? range.max() : range.min();
        return LOCKED_CURSOR.update(range, mc.player.getEyePosition(), mc.player.getLookAngle(), initialDistance);
    }

    private static void moveLockedDistance(MagicBoomStaff staff, double delta) {
        lastLockedMovementMillis = Util.getMillis();
        lockedTarget(staff);
        LOCKED_CURSOR.move(aimRange(staff), delta);
        lockedTarget(staff);
    }

    public static boolean isLockedSelectionMoving() {
        return lastLockedMovementMillis >= 0
                && Util.getMillis() - lastLockedMovementMillis < HIGHLIGHT_SETTLE_MILLIS;
    }

    private static boolean allowed(StaffSelectionBounds bounds, MagicBoomStaff staff) {
        Minecraft mc = Minecraft.getInstance();
        return bounds.fits(staff.maxRange * 2 + 1)
                && bounds.min().getY() >= mc.level.getMinBuildHeight()
                && bounds.max().getY() < mc.level.getMaxBuildHeight()
                && bounds.withinReach(mc.player.getEyePosition(), staff.maxRange * 2 + 1);
    }

    private static void message(String key) {
        Minecraft.getInstance().player.displayClientMessage(Component.translatable("better_experience.staff." + key), true);
    }

    private static void showSize() {
        StaffSelectionBounds bounds = StaffSelectionBounds.of(posA, posB);
        Minecraft.getInstance().player.displayClientMessage(Component.translatable("better_experience.staff.size",
                bounds.max().getX() - bounds.min().getX() + 1,
                bounds.max().getY() - bounds.min().getY() + 1,
                bounds.max().getZ() - bounds.min().getZ() + 1, bounds.manaCost()), true);
    }
}

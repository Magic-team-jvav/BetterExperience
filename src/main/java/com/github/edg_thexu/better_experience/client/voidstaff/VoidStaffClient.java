package com.github.edg_thexu.better_experience.client.voidstaff;

import com.github.edg_thexu.better_experience.item.VoidStaff;
import com.github.edg_thexu.better_experience.module.voidstaff.VoidCargo;
import com.github.edg_thexu.better_experience.module.voidstaff.VoidPlacementRules;
import com.github.edg_thexu.better_experience.networks.c2s.VoidStaffPlacePacket;
import com.github.edg_thexu.better_experience.networks.c2s.VoidStaffStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.UUID;

public final class VoidStaffClient {
    private static VoidCargo cargo;
    private static ResourceLocation dimension;
    private static BlockPos target;
    private static Double distance;
    private static boolean valid;
    private static BlockPos rejected;
    private static int propertyIndex;
    private VoidStaffClient() {}

    public static void synchronize(ResourceLocation currentDimension, VoidCargo value) {
        if (cargo == null || value == null || !cargo.token().equals(value.token())) { distance = null; propertyIndex = 0; rejected = null; }
        cargo = value; dimension = currentDimension;
        VoidStaffRenderer.clear();
    }
    public static VoidCargo cargo() { return cargo; }
    public static BlockPos target() { return target; }
    public static boolean validPlacement() { return valid; }
    public static void reject(UUID token, BlockPos pos) {
        if (cargo != null && cargo.token().equals(token)) { rejected = pos; tick(); }
    }
    public static boolean active() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getMainHandItem().getItem() instanceof VoidStaff && cargo != null;
    }

    public static void tick() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) { cargo = null; target = null; VoidStaffRenderer.clear(); return; }
        if (dimension != null && !dimension.equals(minecraft.level.dimension().location())) { target = null; return; }
        if (!active()) { target = null; return; }
        double reach = player.blockInteractionRange();
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        if (distance == null) {
            var hit = minecraft.level.clip(new ClipContext(eye, eye.add(look.scale(reach)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                target = minecraft.level.getBlockState(hit.getBlockPos()).canBeReplaced() ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
                if (!VoidPlacementRules.withinReach(player, target)) target = VoidPlacementRules.rayTarget(eye, look, reach, reach);
            } else target = VoidPlacementRules.rayTarget(eye, look, reach, reach);
        } else target = VoidPlacementRules.rayTarget(eye, look, Math.min(distance, reach), reach);
        valid = player.mayBuild() && !player.isSpectator() && VoidPlacementRules.withinReach(player, target);
        if (rejected != null && !rejected.equals(target)) rejected = null;
        if (target.equals(rejected)) valid = false;
        for (var part : cargo.parts()) {
            var pos = target.offset(part.offset().rotate(VoidPlacementRules.rotation(cargo.parts().getFirst().state(), cargo.state())));
            var state = part.offset().equals(BlockPos.ZERO) ? cargo.state()
                    : VoidPlacementRules.partState(part, cargo.parts().getFirst().state(), cargo.state());
            valid &= VoidPlacementRules.canPlace(minecraft.level, pos, state);
        }
    }

    public static boolean mouseClicked(int button) {
        var minecraft = Minecraft.getInstance();
        if (!active() || minecraft.screen != null || button != 0) return false;
        if (Screen.hasShiftDown() && target != null) PacketDistributor.sendToServer(new VoidStaffPlacePacket(
                minecraft.player.getInventory().selected, cargo.token(), target, cargo.state()));
        return true;
    }

    public static boolean scroll(double delta) {
        if (!active() || Minecraft.getInstance().screen != null) return false;
        if (delta != 0) moveDistance(delta * 0.25);
        return true;
    }

    public static void moveDistance(double delta) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (distance == null) distance = target == null ? player.blockInteractionRange() : player.getEyePosition().distanceTo(Vec3.atCenterOf(target));
        distance = Mth.clamp(distance + delta, Math.min(0.75, player.blockInteractionRange()), player.blockInteractionRange());
        tick();
    }
    public static void surface() { distance = null; tick(); }
    public static void rotate(int direction) {
        if (cargo != null) changeState(cargo.state().rotate(direction < 0 ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90));
    }
    public static void selectProperty(int delta) {
        if (cargo == null) return;
        int size = VoidPlacementRules.properties(cargo.state()).size();
        propertyIndex = size == 0 ? 0 : Math.floorMod(propertyIndex + delta, size);
    }
    public static Property<?> property() {
        if (cargo == null) return null;
        var properties = VoidPlacementRules.properties(cargo.state());
        return properties.isEmpty() ? null : properties.get(Math.floorMod(propertyIndex, properties.size()));
    }
    public static void cycleValue(int delta) {
        var property = property();
        if (property != null) cycle(property, delta);
    }
    private static <T extends Comparable<T>> void cycle(Property<T> property, int delta) {
        var values = new ArrayList<>(property.getPossibleValues());
        changeState(cargo.state().setValue(property, values.get(Math.floorMod(values.indexOf(cargo.state().getValue(property)) + delta, values.size()))));
    }
    public static String propertyLabel() {
        var property = property();
        return property == null ? "—" : label(property);
    }
    private static <T extends Comparable<T>> String label(Property<T> property) {
        String name = property.getName();
        String value = property.getName(cargo.state().getValue(property));
        String nameKey = "better_experience.space.property." + name;
        String valueKey = "better_experience.space.value." + value;
        return (I18n.exists(nameKey) ? I18n.get(nameKey) : name) + ": " + (I18n.exists(valueKey) ? I18n.get(valueKey) : value);
    }
    private static void changeState(BlockState state) {
        if (cargo == null || !VoidPlacementRules.validState(cargo.parts().getFirst().state(), state)) return;
        cargo = cargo.withState(state);
        rejected = null;
        VoidStaffRenderer.clear();
        PacketDistributor.sendToServer(new VoidStaffStatePacket(Minecraft.getInstance().player.getInventory().selected, cargo.token(), state));
        tick();
    }

    public static void hud(GuiGraphics graphics) {
        if (!active()) return;
        var minecraft = Minecraft.getInstance();
        var message = Component.translatable("better_experience.void_staff.preview", cargo.state().getBlock().getName(),
                Component.translatable("better_experience.void_staff." + (valid ? "ready" : "blocked")));
        graphics.drawCenteredString(minecraft.font, minecraft.font.plainSubstrByWidth(message.getString(), graphics.guiWidth() - 12),
                graphics.guiWidth() / 2, graphics.guiHeight() - 66, valid ? 0xA7E6FF : 0xFF8877);
    }
}

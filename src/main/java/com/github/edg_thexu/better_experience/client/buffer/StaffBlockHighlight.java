package com.github.edg_thexu.better_experience.client.buffer;

import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.MagicBoomStaff;
import com.github.edg_thexu.better_experience.item.SpaceStaff;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffBlockEligibility;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffMiningTools;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import com.github.edg_thexu.better_experience.module.spacestaff.SpaceGeometry;
import com.github.edg_thexu.better_experience.module.spacestaff.SpacePlacementRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** CPU eligibility and shape bounds; GPU instance expansion and pulsing transparency. */
final class StaffBlockHighlight {
    private final List<AABB> surfaces = new ArrayList<>();
    private StaffHighlightInstances instances;
    private StaffSelectionBounds cachedBounds;
    private ClientLevel cachedLevel;
    private ItemStack cachedTool = ItemStack.EMPTY;
    private Item cachedStaff;
    private SpaceStaffSettings cachedSettings;
    private StaffSelectionShape cachedSelection;
    private long cachedTick = Long.MIN_VALUE;

    void clear() {
        if (instances != null) instances.close();
        instances = null;
        surfaces.clear();
        cachedBounds = null;
        cachedLevel = null;
        cachedTool = ItemStack.EMPTY;
        cachedStaff = null;
        cachedSettings = null;
        cachedSelection = null;
        cachedTick = Long.MIN_VALUE;
    }

    void render(StaffSelectionBounds bounds, Matrix4f matrix, Vec3 camera, int r, int g, int b) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clear();
            return;
        }
        ItemStack tool = mc.player.getOffhandItem();
        var held = mc.player.getMainHandItem();
        var settings = held.getItem() instanceof SpaceStaff
                ? held.get(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get()) : null;
        var selection = held.get(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get());
        long tick = mc.level.getGameTime();
        if (cachedLevel != mc.level || !bounds.equals(cachedBounds) || tick != cachedTick
                || held.getItem() != cachedStaff
                || !ItemStack.matches(tool, cachedTool)
                || !Objects.equals(settings, cachedSettings)
                || !Objects.equals(selection, cachedSelection)) {
            cachedSettings = settings;
            cachedSelection = selection;
            rebuild(bounds, held.getItem() instanceof MagicBoomStaff staff ? StaffMiningTools.forPlayer(mc.player, staff) : null);
            cachedBounds = bounds;
            cachedLevel = mc.level;
            cachedTick = tick;
            cachedTool = tool.copy();
            cachedStaff = held.getItem();
        }
        if (instances == null) return;
        int maxSide = Math.max(bounds.max().getX() - bounds.min().getX() + 1,
                Math.max(bounds.max().getY() - bounds.min().getY() + 1,
                        bounds.max().getZ() - bounds.min().getZ() + 1));
        instances.render(matrix, camera, r, g, b, (float) (1 / Math.max(1, maxSide / 6.0)));
    }

    private void rebuild(StaffSelectionBounds bounds, StaffMiningTools tools) {
        List<AABB> nextSurfaces = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        Iterable<BlockPos> positions = cachedSettings != null && cachedSelection != null
                ? SpaceGeometry.positions(bounds, cachedSelection, cachedSettings)
                : BlockPos.betweenClosed(bounds.min(), bounds.max());
        for (BlockPos pos : positions) {
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) continue;
            var state = cachedSettings == null ? level.getBlockState(pos) : cachedSettings.state();
            if (cachedSettings != null) {
                if (!SpacePlacementRules.canPlace(level, pos, state)) continue;
            } else if (tools == null || tools.evaluate(level, pos, state) == StaffBlockEligibility.Result.SKIP) continue;
            var shape = state.getShape(level, pos);
            if (shape.isEmpty()) nextSurfaces.add(new AABB(pos).inflate(.002));
            else for (AABB box : shape.toAabbs()) nextSurfaces.add(box.move(pos).inflate(.002));
        }
        // Unchanged boxes remain resident on the GPU between world checks.
        if (nextSurfaces.equals(surfaces)) return;
        surfaces.clear();
        surfaces.addAll(nextSurfaces);
        if (surfaces.isEmpty()) {
            if (instances != null) instances.close();
            instances = null;
            return;
        }
        if (instances == null) instances = new StaffHighlightInstances();
        instances.upload(surfaces, bounds.min());
    }
}

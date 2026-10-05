package com.github.edg_thexu.better_experience.client.buffer;

import com.github.edg_thexu.better_experience.module.boomstaff.StaffBlockEligibility;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** CPU eligibility and shape bounds; GPU instance expansion and pulsing transparency. */
final class StaffBlockHighlight {
    private final List<AABB> surfaces = new ArrayList<>();
    private StaffHighlightInstances instances;
    private StaffSelectionBounds cachedBounds;
    private Object cachedLevel;
    private ItemStack cachedTool = ItemStack.EMPTY;
    private long cachedTick = Long.MIN_VALUE;

    void clear() {
        if (instances != null) instances.close();
        instances = null;
        surfaces.clear();
        cachedBounds = null;
        cachedLevel = null;
        cachedTool = ItemStack.EMPTY;
        cachedTick = Long.MIN_VALUE;
    }

    void render(StaffSelectionBounds bounds, Matrix4f matrix, Vec3 camera, int r, int g, int b) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clear();
            return;
        }
        ItemStack tool = mc.player.getOffhandItem();
        long tick = mc.level.getGameTime();
        if (cachedLevel != mc.level || !bounds.equals(cachedBounds) || tick != cachedTick
                || !ItemStack.matches(tool, cachedTool)) {
            rebuild(bounds, tool);
            cachedBounds = bounds;
            cachedLevel = mc.level;
            cachedTick = tick;
            cachedTool = tool.copy();
        }
        if (instances == null) return;
        int maxSide = Math.max(bounds.max().getX() - bounds.min().getX() + 1,
                Math.max(bounds.max().getY() - bounds.min().getY() + 1,
                        bounds.max().getZ() - bounds.min().getZ() + 1));
        instances.render(matrix, camera, r, g, b, (float) (1 / Math.max(1, maxSide / 6.0)));
    }

    private void rebuild(StaffSelectionBounds bounds, ItemStack tool) {
        List<AABB> nextSurfaces = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        for (BlockPos pos : BlockPos.betweenClosed(bounds.min(), bounds.max())) {
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (StaffBlockEligibility.evaluate(level, pos, state, tool) == StaffBlockEligibility.Result.SKIP) continue;
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

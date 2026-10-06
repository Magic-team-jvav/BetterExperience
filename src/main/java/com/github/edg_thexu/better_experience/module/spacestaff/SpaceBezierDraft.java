package com.github.edg_thexu.better_experience.module.spacestaff;

import com.github.edg_thexu.better_experience.data.component.SpaceBezierShape;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** One client selection session, with a hovering endpoint that is never saved until clicked. */
public final class SpaceBezierDraft {
    public record Snapshot(StaffSelectionBounds bounds, SpaceBezierShape curve) {}
    private final List<BlockPos> points = new ArrayList<>();
    private BlockPos cachedHover;
    private Snapshot cachedPreview;

    public int size() { return points.size(); }

    public void clear() {
        points.clear();
        cachedPreview = null;
        cachedHover = null;
    }

    public boolean add(BlockPos point) {
        if (points.size() >= SpaceBezierShape.MAX_POINTS || !boundsWith(point).fits(21)) return false;
        points.add(point.immutable());
        cachedPreview = null;
        return true;
    }

    public StaffSelectionBounds boundsWith(BlockPos target) {
        BlockPos min = target == null ? points.getFirst() : target;
        BlockPos max = min;
        for (BlockPos point : points) {
            var bounds = StaffSelectionBounds.of(min, point);
            min = bounds.min();
            max = StaffSelectionBounds.of(max, point).max();
        }
        return StaffSelectionBounds.of(min, max);
    }

    public Snapshot preview(BlockPos target) {
        if (points.isEmpty() && target == null) return null;
        if (points.size() >= SpaceBezierShape.MAX_POINTS) target = null;
        if (cachedPreview != null && Objects.equals(target, cachedHover)) return cachedPreview;
        List<BlockPos> polygon = new ArrayList<>(points);
        if (target != null && (polygon.isEmpty() || !target.equals(polygon.getLast()))) polygon.add(target);
        if (polygon.size() == 1) polygon.add(polygon.getFirst());
        var bounds = boundsWith(target);
        if (!bounds.fits(21)) return null;
        cachedHover = target == null ? null : target.immutable();
        cachedPreview = new Snapshot(bounds, SpaceBezierShape.fromWorld(bounds, polygon));
        return cachedPreview;
    }

    public Snapshot finish() { return points.size() < 2 ? null : preview(null); }
}

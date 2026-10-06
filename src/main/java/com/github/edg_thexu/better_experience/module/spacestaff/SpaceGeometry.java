package com.github.edg_thexu.better_experience.module.spacestaff;

import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Deterministic bounded voxel generation, shared by the GPU preview and server placement. */
public final class SpaceGeometry {
    private SpaceGeometry() {}

    public static List<BlockPos> positions(StaffSelectionBounds bounds, StaffSelectionShape selection,
                                          SpaceStaffSettings settings) {
        if (!bounds.fits(21)) throw new IllegalArgumentException("Selection exceeds space staff range");
        if (settings.shape() == SpaceShape.LINE || settings.shape() == SpaceShape.BEZIER) {
            return curve(bounds, selection, settings);
        }
        List<BlockPos> result = new ArrayList<>();
        SpaceSphere sphere = SpaceSphere.fromBounds(bounds);
        for (BlockPos pos : BlockPos.betweenClosed(bounds.min(), bounds.max())) {
            boolean include = switch (settings.shape()) {
                case CUBE -> true;
                case HOLLOW_CUBE -> pos.getX() == bounds.min().getX() || pos.getX() == bounds.max().getX()
                        || pos.getY() == bounds.min().getY() || pos.getY() == bounds.max().getY()
                        || pos.getZ() == bounds.min().getZ() || pos.getZ() == bounds.max().getZ();
                case SPHERE -> sphere.contains(pos);
                case HOLLOW_SPHERE -> sphere.contains(pos) && sphere.isShell(pos);
                default -> false;
            };
            if (include) result.add(pos.immutable());
        }
        return result;
    }

    public static Vec3 control(StaffSelectionBounds bounds, BlockPos percent) {
        return new Vec3(bounds.min().getX() + .5 + (bounds.max().getX() - bounds.min().getX()) * percent.getX() / 100.0,
                bounds.min().getY() + .5 + (bounds.max().getY() - bounds.min().getY()) * percent.getY() / 100.0,
                bounds.min().getZ() + .5 + (bounds.max().getZ() - bounds.min().getZ()) * percent.getZ() / 100.0);
    }

    private static List<BlockPos> curve(StaffSelectionBounds bounds, StaffSelectionShape selection,
                                        SpaceStaffSettings settings) {
        LinkedHashSet<BlockPos> result = new LinkedHashSet<>();
        for (Vec3 point : path(bounds, selection, settings)) result.add(BlockPos.containing(point));
        return List.copyOf(result);
    }

    public static List<Vec3> path(StaffSelectionBounds bounds, StaffSelectionShape selection,
                                  SpaceStaffSettings settings) {
        return sampleBezier(controlPolygon(bounds, selection, settings));
    }

    public static List<Vec3> controlPolygon(StaffSelectionBounds bounds, StaffSelectionShape selection,
                                            SpaceStaffSettings settings) {
        if (settings.shape() == SpaceShape.BEZIER && settings.worldCurve().isPresent()) {
            return settings.worldCurve().get().polygon(bounds);
        }
        BlockPos offset = selection.offset();
        Vec3 start = new Vec3((offset.getX() < 0 ? bounds.max().getX() : bounds.min().getX()) + .5,
                (offset.getY() < 0 ? bounds.max().getY() : bounds.min().getY()) + .5,
                (offset.getZ() < 0 ? bounds.max().getZ() : bounds.min().getZ()) + .5);
        Vec3 end = new Vec3((offset.getX() < 0 ? bounds.min().getX() : bounds.max().getX()) + .5,
                (offset.getY() < 0 ? bounds.min().getY() : bounds.max().getY()) + .5,
                (offset.getZ() < 0 ? bounds.min().getZ() : bounds.max().getZ()) + .5);
        List<Vec3> polygon = new ArrayList<>();
        polygon.add(start);
        if (settings.shape() == SpaceShape.BEZIER) {
            for (BlockPos point : settings.controls()) polygon.add(control(bounds, point));
        }
        polygon.add(end);
        return List.copyOf(polygon);
    }

    /** De Casteljau evaluation stays stable even with repeated controls or a high polynomial degree. */
    public static Vec3 bezierPoint(List<Vec3> polygon, double t) {
        if (polygon.size() < 2 || polygon.size() > SpaceStaffSettings.MAX_CONTROLS + 2
                || !Double.isFinite(t) || t < 0 || t > 1) {
            throw new IllegalArgumentException("Invalid Bezier polygon or parameter");
        }
        if (t == 0) return polygon.getFirst();
        if (t == 1) return polygon.getLast();
        double[] x = new double[polygon.size()];
        double[] y = new double[polygon.size()];
        double[] z = new double[polygon.size()];
        for (int i = 0; i < polygon.size(); i++) {
            x[i] = polygon.get(i).x;
            y[i] = polygon.get(i).y;
            z[i] = polygon.get(i).z;
        }
        for (int remaining = polygon.size() - 1; remaining > 0; remaining--) {
            for (int i = 0; i < remaining; i++) {
                x[i] += (x[i + 1] - x[i]) * t;
                y[i] += (y[i + 1] - y[i]) * t;
                z[i] += (z[i + 1] - z[i]) * t;
            }
        }
        return new Vec3(x[0], y[0], z[0]);
    }

    private static List<Vec3> sampleBezier(List<Vec3> polygon) {
        double maxEdge = 0;
        for (int i = 1; i < polygon.size(); i++) {
            maxEdge = Math.max(maxEdge, polygon.get(i - 1).distanceTo(polygon.get(i)));
        }
        // Degree * longest edge bounds the derivative, preventing gaps at tightly bunched controls.
        int samples = Math.max(1, (int) Math.ceil(4 * (polygon.size() - 1) * maxEdge));
        List<Vec3> result = new ArrayList<>(samples + 1);
        for (int i = 0; i <= samples; i++) result.add(bezierPoint(polygon, (double) i / samples));
        return List.copyOf(result);
    }
}

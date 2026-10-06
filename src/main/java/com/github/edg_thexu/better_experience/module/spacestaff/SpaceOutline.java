package com.github.edg_thexu.better_experience.module.spacestaff;

import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Lightweight analytic guides, generated only when the selection changes. No world/block scanning. */
public final class SpaceOutline {
    public enum Guide { SHAPE, CENTER, HORIZONTAL_RADIUS, VERTICAL_RADIUS, CONTROL }
    public record Segment(Vec3 from, Vec3 to, Guide guide) {}
    private static final int CIRCLE_STEPS = 64;

    private SpaceOutline() {}

    public static List<Segment> segments(StaffSelectionBounds bounds, StaffSelectionShape selection,
                                         SpaceStaffSettings settings) {
        List<Segment> result = new ArrayList<>();
        switch (settings.shape()) {
            case CUBE, HOLLOW_CUBE -> cube(result, bounds, settings.shape() == SpaceShape.HOLLOW_CUBE);
            case SPHERE, HOLLOW_SPHERE -> sphere(result, bounds, settings.shape() == SpaceShape.HOLLOW_SPHERE);
            case LINE, BEZIER -> {
                var points = SpaceGeometry.path(bounds, selection, settings);
                for (int i = 1; i < points.size(); i++) result.add(new Segment(points.get(i - 1), points.get(i), Guide.SHAPE));
                marker(result, points.getFirst(), .12, Guide.CENTER);
                marker(result, points.getLast(), .12, Guide.CENTER);
                if (settings.shape() == SpaceShape.BEZIER) {
                    var polygon = SpaceGeometry.controlPolygon(bounds, selection, settings);
                    for (int i = 1; i < polygon.size(); i++) {
                        Vec3 point = polygon.get(i);
                        result.add(new Segment(polygon.get(i - 1), point, Guide.CONTROL));
                        if (i == polygon.size() - 1) continue;
                        marker(result, point, .1, Guide.CONTROL);
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static void cube(List<Segment> result, StaffSelectionBounds bounds, boolean hollow) {
        Vec3 min = new Vec3(bounds.min().getX(), bounds.min().getY(), bounds.min().getZ());
        Vec3 max = new Vec3(bounds.max().getX() + 1, bounds.max().getY() + 1, bounds.max().getZ() + 1);
        box(result, min, max);
        if (hollow && bounds.max().getX() - bounds.min().getX() > 1
                && bounds.max().getY() - bounds.min().getY() > 1
                && bounds.max().getZ() - bounds.min().getZ() > 1) box(result, min.add(1, 1, 1), max.add(-1, -1, -1));
    }

    private static void box(List<Segment> result, Vec3 min, Vec3 max) {
        for (int axis = 0; axis < 3; axis++) {
            for (int mask = 0; mask < 4; mask++) {
                double[] a = {min.x, min.y, min.z};
                double[] b = {min.x, min.y, min.z};
                double[] high = {max.x, max.y, max.z};
                b[axis] = high[axis];
                int index = 0;
                for (int other = 0; other < 3; other++) {
                    if (other == axis) continue;
                    if ((mask & (1 << index++)) != 0) a[other] = b[other] = high[other];
                }
                result.add(new Segment(new Vec3(a[0], a[1], a[2]), new Vec3(b[0], b[1], b[2]), Guide.SHAPE));
            }
        }
    }

    private static void sphere(List<Segment> result, StaffSelectionBounds bounds, boolean hollow) {
        var sphere = SpaceSphere.fromBounds(bounds);
        Vec3 center = sphere.center();
        double horizontal = sphere.horizontalRadius();
        double vertical = sphere.verticalRadius();
        for (int ring = -2; ring <= 2; ring++) {
            double latitude = ring * Math.PI / 6;
            circle(result, center.add(0, vertical * Math.sin(latitude), 0),
                    horizontal * Math.cos(latitude), horizontal * Math.cos(latitude), 0);
        }
        for (int axis = 1; axis <= 2; axis++) circle(result, center, horizontal, vertical, axis);
        if (hollow && horizontal > 1 && vertical > 1) {
            circle(result, center, horizontal - 1, horizontal - 1, 0);
            circle(result, center, horizontal - 1, vertical - 1, 1);
        }
        marker(result, center, .16, Guide.CENTER);
        result.add(new Segment(center, center.add(horizontal, 0, 0), Guide.HORIZONTAL_RADIUS));
        result.add(new Segment(center, center.add(0, vertical, 0), Guide.VERTICAL_RADIUS));
        marker(result, center.add(horizontal, 0, 0), .12, Guide.HORIZONTAL_RADIUS);
        marker(result, center.add(0, vertical, 0), .12, Guide.VERTICAL_RADIUS);
    }

    private static void circle(List<Segment> result, Vec3 center, double a, double b, int plane) {
        Vec3 previous = circlePoint(center, a, b, plane, 0);
        for (int i = 1; i <= CIRCLE_STEPS; i++) {
            Vec3 next = circlePoint(center, a, b, plane, i * Math.PI * 2 / CIRCLE_STEPS);
            result.add(new Segment(previous, next, Guide.SHAPE));
            previous = next;
        }
    }

    private static Vec3 circlePoint(Vec3 center, double a, double b, int plane, double angle) {
        double u = a * Math.cos(angle);
        double v = b * Math.sin(angle);
        return center.add(plane == 2 ? 0 : u, plane == 0 ? 0 : v, plane == 0 ? v : plane == 2 ? u : 0);
    }

    private static void marker(List<Segment> result, Vec3 point, double radius, Guide guide) {
        result.add(new Segment(point.add(-radius, 0, 0), point.add(radius, 0, 0), guide));
        result.add(new Segment(point.add(0, -radius, 0), point.add(0, radius, 0), guide));
        result.add(new Segment(point.add(0, 0, -radius), point.add(0, 0, radius), guide));
    }
}

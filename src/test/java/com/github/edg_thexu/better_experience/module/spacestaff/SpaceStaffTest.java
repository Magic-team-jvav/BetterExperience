package com.github.edg_thexu.better_experience.module.spacestaff;

import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.client.gui.SpaceWheelLayout;
import com.github.edg_thexu.better_experience.client.gui.SpacePreviewTransform;
import com.github.edg_thexu.better_experience.client.gui.SpaceStaffUiTest;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class SpaceStaffTest {
    public static void main(String[] args) {
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SpaceStaffUiTest.verify();
        SpaceBezierSelectionTest.verify();
        var wheel = new SpaceWheelLayout(34, 94, SpaceShape.values().length);
        for (int index = 0; index < SpaceShape.values().length; index++) {
            double angle = wheel.centerAngle(index);
            require(wheel.sectorAt(Math.cos(angle) * 64, Math.sin(angle) * 64) == index,
                    "wheel drawn sector matches mouse selection");
            require(wheel.sectorAt(Math.cos(angle - 0.50) * 64, Math.sin(angle - 0.50) * 64) == index
                    && wheel.sectorAt(Math.cos(angle + 0.50) * 64, Math.sin(angle + 0.50) * 64) == index,
                    "wheel sector edges and angular wrap");
        }
        require(wheel.sectorAt(0, 0) == -1 && wheel.sectorAt(34, 0) == -1
                && wheel.sectorAt(95, 0) == -1 && wheel.sectorAt(Double.NaN, 0) == -1,
                "wheel center, exterior and invalid coordinates never select a shape");
        for (int size : new int[]{36, 40, 44, 64}) {
            var model = SpacePreviewTransform.model(242, 3, size);
            var center = model.transformPosition(new org.joml.Vector3f(0.5F, 0.5F, 0.5F));
            require(Math.abs(center.x - (242 + size / 2F)) < 0.001F
                    && Math.abs(center.y - (3 + size / 2F)) < 0.001F,
                    "block preview centered in every layout");
            for (int corner = 0; corner < 8; corner++) {
                var point = model.transformPosition(new org.joml.Vector3f(corner & 1, (corner >> 1) & 1, (corner >> 2) & 1));
                require(point.x > 243 && point.x < 242 + size - 1
                        && point.y > 4 && point.y < 3 + size - 1, "preview cube fits inside scissor");
            }
            // The front-facing south face must remain counterclockwise in OpenGL NDC.
            var projection = new org.joml.Matrix4f().ortho(0, 320, 240, 0, 1000, 21000).mul(model);
            var a = projection.transformPosition(new org.joml.Vector3f(0, 0, 1));
            var b = projection.transformPosition(new org.joml.Vector3f(1, 0, 1));
            var c = projection.transformPosition(new org.joml.Vector3f(1, 1, 1));
            require((b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x) > 0,
                    "GUI projection preserves visible face winding");
        }
        StaffSelectionBounds cube = StaffSelectionBounds.of(BlockPos.ZERO, new BlockPos(4, 4, 4));
        StaffSelectionShape selection = StaffSelectionShape.fromCorners(cube.min(), cube.max());
        var defaults = SpaceStaffSettings.defaults();
        for (SpaceShape shape : SpaceShape.values()) {
            var settings = settings(shape);
            List<BlockPos> positions = SpaceGeometry.positions(cube, selection, settings);
            require(!positions.isEmpty() && positions.size() == new HashSet<>(positions).size(), shape + " unique");
            require(positions.stream().allMatch(pos -> inside(cube, pos)), shape + " bounded");
            switch (shape) {
                case CUBE -> require(positions.size() == 125, "solid cube volume");
                case HOLLOW_CUBE -> require(positions.size() == 98 && !positions.contains(new BlockPos(2, 2, 2)), "hollow cube");
                case SPHERE -> require(positions.size() == 81 && positions.contains(new BlockPos(2, 2, 2)), "sphere");
                case HOLLOW_SPHERE -> require(positions.size() == 54 && !positions.contains(new BlockPos(2, 2, 2)), "sphere shell");
                case LINE, BEZIER -> {
                    require(positions.getFirst().equals(cube.min()) && positions.getLast().equals(cube.max()), "curve endpoints");
                    for (int i = 1; i < positions.size(); i++) {
                        BlockPos step = positions.get(i).subtract(positions.get(i - 1));
                        require(Math.abs(step.getX()) <= 1 && Math.abs(step.getY()) <= 1 && Math.abs(step.getZ()) <= 1, "continuous curve");
                    }
                }
            }
            BlockPos translation = new BlockPos(-100000, 50, 100000);
            var moved = StaffSelectionBounds.of(cube.min().offset(translation), cube.max().offset(translation));
            var movedPositions = SpaceGeometry.positions(moved, selection, settings);
            require(new HashSet<>(movedPositions).equals(new HashSet<>(positions.stream().map(pos -> pos.offset(translation)).toList())), "translation invariant");
            var single = StaffSelectionBounds.of(BlockPos.ZERO, BlockPos.ZERO);
            require(SpaceGeometry.positions(single, new StaffSelectionShape(BlockPos.ZERO), settings).equals(List.of(BlockPos.ZERO)), "single voxel");
        }
        var reversed = new StaffSelectionShape(new BlockPos(-4, -4, -4));
        require(SpaceGeometry.positions(cube, reversed, settings(SpaceShape.LINE)).getFirst().equals(cube.max()), "signed endpoints");
        var rectangle = StaffSelectionBounds.of(BlockPos.ZERO, new BlockPos(4, 2, 6));
        require(SpaceGeometry.positions(rectangle, new StaffSelectionShape(rectangle.max()), settings(SpaceShape.SPHERE)).size() == 39, "independent sphere radii");
        var sphereBounds = SpaceSphere.bounds(new BlockPos(10, 30, -20), 4, 2);
        var sphere = SpaceSphere.fromBounds(sphereBounds);
        require(sphere.center().equals(new net.minecraft.world.phys.Vec3(10.5, 30.5, -19.5))
                && sphere.horizontalRadius() == 4.5 && sphere.verticalRadius() == 2.5, "center and radii");
        require(SpaceSphere.horizontalBlocks(BlockPos.ZERO, new BlockPos(3, 99, 4)) == 5
                && SpaceSphere.verticalBlocks(BlockPos.ZERO, new BlockPos(99, 2, 99)) == 2, "independent selection axes");
        for (SpaceShape shape : SpaceShape.values()) {
            var lines = SpaceOutline.segments(sphereBounds, new StaffSelectionShape(sphereBounds.max().subtract(sphereBounds.min())), settings(shape));
            require(!lines.isEmpty(), "shape outline present");
            if (shape == SpaceShape.SPHERE || shape == SpaceShape.HOLLOW_SPHERE) {
                require(lines.stream().anyMatch(line -> line.guide() == SpaceOutline.Guide.HORIZONTAL_RADIUS
                                && line.from().equals(sphere.center()) && line.to().equals(sphere.center().add(4.5, 0, 0))),
                        "horizontal guide matches placement");
                require(lines.stream().anyMatch(line -> line.guide() == SpaceOutline.Guide.VERTICAL_RADIUS
                                && line.from().equals(sphere.center()) && line.to().equals(sphere.center().add(0, 2.5, 0))),
                        "vertical guide matches placement");
            }
        }
        var largest = StaffSelectionBounds.of(BlockPos.ZERO, new BlockPos(20, 20, 20));
        require(SpaceGeometry.positions(largest, new StaffSelectionShape(largest.max()), defaults).size() == 9261, "selection cap");

        var doubleSlab = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
        require(SpacePlacementRules.materialCost(doubleSlab) == 2, "double slab materials");
        require(SpacePlacementRules.materialCost(Blocks.STONE.defaultBlockState()) == 1, "ordinary material");
        require(!SpacePlacementRules.supported(Blocks.OAK_DOOR.defaultBlockState())
                && !SpacePlacementRules.supported(Blocks.WHITE_BED.defaultBlockState()), "multi-block items");
        require(SpacePlacementRules.isMaterial(new ItemStack(Items.STONE), Blocks.STONE.defaultBlockState())
                && !SpacePlacementRules.isMaterial(new ItemStack(Items.DIRT), Blocks.STONE.defaultBlockState()), "material match");

        var custom = new SpaceStaffSettings(SpaceShape.BEZIER, doubleSlab, List.of(new BlockPos(0, 100, 50), new BlockPos(100, 0, 50)));
        var encoded = SpaceStaffSettings.CODEC.encodeStart(JsonOps.INSTANCE, custom).getOrThrow();
        require(SpaceStaffSettings.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(custom), "state and control persistence");
        encoded.getAsJsonObject().addProperty("shape", "invalid");
        require(SpaceStaffSettings.CODEC.parse(JsonOps.INSTANCE, encoded).error().isPresent(), "invalid mode rejected");
        require(!SpaceStaffSettings.validControl(new BlockPos(-1, 0, 0))
                && !SpaceStaffSettings.validControl(new BlockPos(101, 50, 50)), "control range");
        verifyMultipoint(cube, selection, doubleSlab);
        System.out.println("Space staff checks passed (six shapes, multipoint Bezier, legacy migration, continuity, translation and material costs).");
    }

    private static void verifyMultipoint(StaffSelectionBounds bounds, StaffSelectionShape selection,
                                          net.minecraft.world.level.block.state.BlockState state) {
        var polygon = List.of(new net.minecraft.world.phys.Vec3(0, 0, 0),
                new net.minecraft.world.phys.Vec3(0, 4, 0),
                new net.minecraft.world.phys.Vec3(4, 4, 4));
        require(SpaceGeometry.bezierPoint(polygon, .5).equals(new net.minecraft.world.phys.Vec3(1, 3, 1)), "quadratic reference");

        var cubic = List.of(new net.minecraft.world.phys.Vec3(0, 0, 0),
                new net.minecraft.world.phys.Vec3(0, 4, 0), new net.minecraft.world.phys.Vec3(4, 4, 4),
                new net.minecraft.world.phys.Vec3(4, 0, 4));
        require(SpaceGeometry.bezierPoint(cubic, .5).equals(new net.minecraft.world.phys.Vec3(2, 3, 2)), "legacy cubic reference");
        var many = new java.util.ArrayList<BlockPos>();
        for (int i = 0; i < SpaceStaffSettings.MAX_CONTROLS; i++) {
            many.add(new BlockPos(i % 2 == 0 ? 0 : 100, i % 3 == 0 ? 100 : 0, (i * 7) % 101));
        }
        for (int count : new int[]{0, 1, 2, 5, SpaceStaffSettings.MAX_CONTROLS}) {
            var settings = new SpaceStaffSettings(SpaceShape.BEZIER, state, many.subList(0, count));
            var path = SpaceGeometry.path(bounds, selection, settings);
            require(path.getFirst().equals(net.minecraft.world.phys.Vec3.atCenterOf(bounds.min()))
                    && path.getLast().equals(net.minecraft.world.phys.Vec3.atCenterOf(bounds.max())), "multipoint endpoints");
            var voxels = SpaceGeometry.positions(bounds, selection, settings);
            require(voxels.stream().allMatch(pos -> inside(bounds, pos)), "multipoint bounded");
            for (int i = 1; i < path.size(); i++) require(path.get(i).distanceTo(path.get(i - 1)) <= .251, "derivative sampling bound");
            var json = SpaceStaffSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow();
            require(SpaceStaffSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(settings), "multipoint persistence");
            var outlines = SpaceOutline.segments(bounds, selection, settings);
            require(outlines.stream().filter(line -> line.guide() == SpaceOutline.Guide.CONTROL).count()
                    == count * 4L + 1, "all control markers and polygon edges");
        }
        var input = new java.util.ArrayList<>(many);
        var snapshot = new SpaceStaffSettings(SpaceShape.BEZIER, state, input);
        input.clear();
        require(snapshot.controls().size() == SpaceStaffSettings.MAX_CONTROLS, "immutable controls");

        var legacy = SpaceStaffSettings.CODEC.encodeStart(JsonOps.INSTANCE, SpaceStaffSettings.defaults()).getOrThrow().getAsJsonObject();
        legacy.remove("control_points");
        legacy.add("control_a", BlockPos.CODEC.encodeStart(JsonOps.INSTANCE, new BlockPos(10, 20, 30)).getOrThrow());
        legacy.add("control_b", BlockPos.CODEC.encodeStart(JsonOps.INSTANCE, new BlockPos(70, 80, 90)).getOrThrow());
        require(SpaceStaffSettings.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow().controls()
                .equals(List.of(new BlockPos(10, 20, 30), new BlockPos(70, 80, 90))), "legacy controls migration");
        var bad = SpaceStaffSettings.CODEC.encodeStart(JsonOps.INSTANCE, snapshot).getOrThrow().getAsJsonObject();
        bad.getAsJsonArray("control_points").add(BlockPos.CODEC.encodeStart(JsonOps.INSTANCE, BlockPos.ZERO).getOrThrow());
        require(SpaceStaffSettings.CODEC.parse(JsonOps.INSTANCE, bad).error().isPresent(), "too many controls rejected");
        bad.getAsJsonArray("control_points").remove(bad.getAsJsonArray("control_points").size() - 1);
        bad.getAsJsonArray("control_points").set(0, BlockPos.CODEC.encodeStart(JsonOps.INSTANCE, new BlockPos(101, 0, 0)).getOrThrow());
        require(SpaceStaffSettings.CODEC.parse(JsonOps.INSTANCE, bad).error().isPresent(), "invalid coordinate rejected");
    }

    private static SpaceStaffSettings settings(SpaceShape shape) {
        var defaults = SpaceStaffSettings.defaults();
        return new SpaceStaffSettings(shape, defaults.state(), defaults.controls());
    }

    private static boolean inside(StaffSelectionBounds bounds, BlockPos pos) {
        return pos.getX() >= bounds.min().getX() && pos.getX() <= bounds.max().getX()
                && pos.getY() >= bounds.min().getY() && pos.getY() <= bounds.max().getY()
                && pos.getZ() >= bounds.min().getZ() && pos.getZ() <= bounds.max().getZ();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

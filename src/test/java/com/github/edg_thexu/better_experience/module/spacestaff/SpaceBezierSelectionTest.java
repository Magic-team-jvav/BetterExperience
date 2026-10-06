package com.github.edg_thexu.better_experience.module.spacestaff;

import com.github.edg_thexu.better_experience.data.component.SpaceBezierShape;
import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import com.mojang.serialization.JsonOps;
import com.github.edg_thexu.better_experience.networks.c2s.SpaceBezierSelectionPacketC2S;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.List;

public final class SpaceBezierSelectionTest {
    private SpaceBezierSelectionTest() {}

    public static void verify() {
        var draft = new SpaceBezierDraft();
        require(draft.preview(null) == null && draft.finish() == null, "empty draft has no lockable curve");
        var clicked = List.of(new BlockPos(2, 10, 3), new BlockPos(8, 14, 9), new BlockPos(-2, 12, -1),
                new BlockPos(1, 8, 5), new BlockPos(4, 11, 4));
        for (BlockPos point : clicked) require(draft.add(point), "all world points can be selected after point two");
        var hover = draft.preview(new BlockPos(6, 9, 7));
        require(hover.curve().points().size() == 6 && draft.size() == 5, "hover endpoint doesn't commit a point");
        require(hover == draft.preview(new BlockPos(6, 9, 7)), "stationary preview is cached");
        var locked = draft.finish();
        require(locked.curve().points().size() == 5, "right click locks only confirmed points");
        var selection = new StaffSelectionShape(locked.curve().span());
        var settings = SpaceStaffSettings.defaults().withShape(SpaceShape.BEZIER).withCurve(locked.curve());
        require(SpaceGeometry.controlPolygon(locked.bounds(), selection, settings)
                .equals(clicked.stream().map(Vec3::atCenterOf).toList()), "exact world polygon survives bounding box conversion");
        var path = SpaceGeometry.path(locked.bounds(), selection, settings);
        require(path.getFirst().equals(Vec3.atCenterOf(clicked.getFirst()))
                && path.getLast().equals(Vec3.atCenterOf(clicked.getLast())), "endpoints remain inside the box, not forced to its corners");
        var positions = SpaceGeometry.positions(locked.bounds(), selection, settings);
        require(positions.getFirst().equals(clicked.getFirst()) && positions.contains(clicked.getLast()), "placement includes the exact endpoints");
        requireConnected(positions);
        for (int i = 1; i < path.size(); i++) require(path.get(i).distanceTo(path.get(i - 1)) <= .251, "world curve sampling has no gaps");
        var outlines = SpaceOutline.segments(locked.bounds(), selection, settings);
        require(outlines.stream().anyMatch(line -> line.guide() == SpaceOutline.Guide.CONTROL
                && line.from().equals(Vec3.atCenterOf(clicked.get(1))) && line.to().equals(Vec3.atCenterOf(clicked.get(2)))),
                "control guide uses actual clicked points");
        BlockPos delta = new BlockPos(100, -20, -200);
        var moved = StaffSelectionBounds.of(locked.bounds().min().offset(delta), locked.bounds().max().offset(delta));
        require(new HashSet<>(SpaceGeometry.positions(moved, selection, settings))
                .equals(new HashSet<>(positions.stream().map(point -> point.offset(delta)).toList())), "locked curve moves as one shape");
        var json = SpaceStaffSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow();
        require(SpaceStaffSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(settings), "exact polygon and block state persist");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            var packet = new SpaceBezierSelectionPacketC2S(3, locked.curve());
            SpaceBezierSelectionPacketC2S.STREAM_CODEC.encode(buffer, packet);
            require(SpaceBezierSelectionPacketC2S.STREAM_CODEC.decode(buffer).equals(packet) && buffer.readableBytes() == 0,
                    "atomic selection packet round trip");
            SpaceStaffSettings.STREAM_CODEC.encode(buffer, settings);
            require(SpaceStaffSettings.STREAM_CODEC.decode(buffer).equals(settings), "item component network sync retains exact points");
        } finally { buffer.release(); }
        require(settings.edited(SpaceShape.BEZIER, Blocks.DIRT.defaultBlockState(), settings.controls()).worldCurve()
                .equals(settings.worldCurve()), "changing material preserves exact clicked points");
        var edited = settings.edited(SpaceShape.BEZIER, settings.state(), List.of());
        var editedPolygon = SpaceGeometry.controlPolygon(locked.bounds(), selection, edited);
        require(editedPolygon.equals(List.of(Vec3.atCenterOf(clicked.getFirst()), Vec3.atCenterOf(clicked.getLast()))),
                "editing controls preserves clicked endpoints");
        require(settings.withShape(SpaceShape.CUBE).withShape(SpaceShape.BEZIER).worldCurve().equals(settings.worldCurve()),
                "shape wheel preserves saved curve");

        var invalid = SpaceBezierShape.CODEC.encodeStart(JsonOps.INSTANCE, locked.curve()).getOrThrow().getAsJsonObject();
        invalid.add("span", BlockPos.CODEC.encodeStart(JsonOps.INSTANCE, BlockPos.ZERO).getOrThrow());
        require(SpaceBezierShape.CODEC.parse(JsonOps.INSTANCE, invalid).error().isPresent(), "out-of-frame packet points are rejected without throwing");
        var capped = new SpaceBezierDraft();
        require(capped.add(BlockPos.ZERO) && !capped.add(new BlockPos(21, 0, 0)) && capped.size() == 1, "21-block side limit retains last points");
        require(capped.finish() == null && capped.preview(new BlockPos(21, 0, 0)) == null, "invalid hover never becomes a curve");
        for (int i = 1; i < SpaceBezierShape.MAX_POINTS; i++) require(capped.add(new BlockPos(i % 2, 0, 0)), "18 points accepted");
        require(!capped.add(BlockPos.ZERO) && capped.finish().curve().points().size() == 18
                && capped.preview(new BlockPos(100, 100, 100)).equals(capped.finish()), "point cap freezes completed preview");
        capped.clear();
        require(capped.size() == 0 && capped.finish() == null && capped.preview(null) == null, "cancel clears the draft and its cache");
        var mutable = new BlockPos.MutableBlockPos(1, 2, 3);
        require(capped.add(mutable), "mutable target accepted");
        mutable.set(20, 20, 20);
        require(capped.add(new BlockPos(2, 2, 3))
                && capped.finish().bounds().min().equals(new BlockPos(1, 2, 3)), "picked points are immutable snapshots");
    }

    private static void requireConnected(List<BlockPos> positions) {
        var remaining = new HashSet<>(positions);
        var queue = new ArrayDeque<BlockPos>();
        queue.add(positions.getFirst());
        remaining.remove(positions.getFirst());
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                BlockPos next = current.offset(x, y, z);
                if (remaining.remove(next)) queue.addLast(next);
            }
        }
        require(remaining.isEmpty(), "selected curve voxels form one continuous path even with repeated visits");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

package com.github.edg_thexu.better_experience.module.boomstaff;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import com.github.edg_thexu.better_experience.networks.c2s.BreakBlocksPacketC2S;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Set;

/** Standalone regression checks; run main with the Minecraft development classpath. */
public class StaffSelectionMathTest {
    public static void main(String[] args) {
        Set<StaffSelectionDirection> directions = new HashSet<>();
        for (int yaw = -180; yaw < 180; yaw++) {
            for (int pitch = -90; pitch <= 90; pitch++) {
                var d = StaffSelectionDirection.fromRotation(yaw, pitch);
                int axes = Math.abs(d.x()) + Math.abs(d.y()) + Math.abs(d.z());
                check(axes == 1 || axes == 2, "no corner or zero directions");
                directions.add(d);
            }
        }
        check(directions.size() == 18, "exactly eighteen directions");
        check(StaffSelectionDirection.fromRotation(0, 0).equals(new StaffSelectionDirection(0, 0, 1)), "south");
        check(StaffSelectionDirection.fromRotation(90, 0).equals(new StaffSelectionDirection(-1, 0, 0)), "west");
        check(StaffSelectionDirection.fromRotation(0, -90).equals(new StaffSelectionDirection(0, 1, 0)), "up");
        check(StaffSelectionDirection.fromRotation(0, 90).equals(new StaffSelectionDirection(0, -1, 0)), "down");
        var point = StaffSelectionBounds.of(BlockPos.ZERO, BlockPos.ZERO);
        check(point.volume() == 1 && point.manaCost() == 1, "inclusive single block");
        var cube = StaffSelectionBounds.of(new BlockPos(10, 10, 10), BlockPos.ZERO);
        check(cube.volume() == 1331 && cube.fits(11) && !cube.fits(10), "reversed corners and magic staff limit");
        check(cube.manaCost() == 167, "rounded mana for magic staff maximum");
        var star = StaffSelectionBounds.of(BlockPos.ZERO, new BlockPos(20, 20, 20));
        check(star.fits(21) && star.manaCost() == 1158, "star staff maximum");
        check(StaffSelectionBounds.of(BlockPos.ZERO, new BlockPos(21, 0, 0)).fits(21) == false, "oversized rejection");
        check(StaffSelectionBounds.of(BlockPos.ZERO, new BlockPos(7, 0, 0)).manaCost() == 1, "eight blocks cost one");
        check(StaffSelectionBounds.of(BlockPos.ZERO, new BlockPos(8, 0, 0)).manaCost() == 2, "nine blocks round up");
        for (var direction : directions) {
            var expanded = point.adjust(direction, 1, 11);
            check(expanded != null && expanded.fits(11), "expansion in each direction");
            check(expanded.adjust(direction, -1, 11).equals(point), "contraction reverses expansion");
            check(point.adjust(direction, -1, 11) == null, "cannot collapse a single block");
            check(cube.adjust(direction, 1, 11) == null, "all boundaries enforce the limit");
        }
        var huge = StaffSelectionBounds.of(new BlockPos(Integer.MIN_VALUE, 0, 0), new BlockPos(Integer.MAX_VALUE, 0, 0));
        check(!huge.fits(21), "coordinate subtraction cannot overflow validation");
        var crossed = StaffSelectionBounds.of(new BlockPos(10, 0, 0), new BlockPos(0, 0, 10));
        check(!crossed.withinReach(new Vec3(.5, .5, .5), 11), "check mixed farthest corners too");
        check(crossed.withinReach(new Vec3(5.5, .5, 5.5), 11), "same selection in range when closer");
        var shape = StaffSelectionShape.fromCorners(new BlockPos(10, 5, -10), new BlockPos(7, 7, -6));
        check(shape.offset().equals(new BlockPos(-3, 2, 4)), "saved shape retains signed offsets");
        var relocated = shape.at(new BlockPos(100, 20, 200));
        check(relocated.min().equals(new BlockPos(97, 20, 200)), "new aimed point is pos1 with negative offsets");
        check(relocated.max().equals(new BlockPos(100, 22, 204)), "shape translated to the new aimed point");
        check(relocated.volume() == 60 && relocated.manaCost() == 8, "relocation preserves size and mana");
        var centered = shape.centeredAt(new Vec3(100, 21.5, 202.5));
        check(centered.min().equals(new BlockPos(98, 20, 200))
                && centered.max().equals(new BlockPos(101, 22, 204)), "mixed odd and even sides use geometric center");
        check(centered.volume() == relocated.volume() && centered.manaCost() == relocated.manaCost(),
                "centered placement preserves size and mana");
        var reversedShape = new StaffSelectionShape(new BlockPos(3, -2, -4));
        check(reversedShape.centeredAt(new Vec3(100, 21.5, 202.5)).equals(centered),
                "centered placement does not depend on corner ordering");
        check(shape.centeredAt(new Vec3(-100, -21.5, -202.5)).min().equals(new BlockPos(-102, -23, -205)),
                "negative coordinates snap consistently");
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    var savedShape = new StaffSelectionShape(new BlockPos(x, y, z));
                    var displayed = savedShape.centeredAt(new Vec3(-100.2, 21.75, -202.6));
                    var packetBytes = Unpooled.buffer();
                    try {
                        BreakBlocksPacketC2S.STREAM_CODEC.encode(packetBytes, new BreakBlocksPacketC2S(displayed.center(), 3));
                        var received = BreakBlocksPacketC2S.STREAM_CODEC.decode(packetBytes);
                        check(received.slot() == 3 && savedShape.centeredAt(received.center()).equals(displayed),
                                "blast packet restores exact displayed bounds for all side parities and offset signs");
                    } finally { packetBytes.release(); }
                }
            }
        }
        BlockPos moveOrigin = new BlockPos(100, 20, 200);
        for (var direction : directions) {
            BlockPos forward = direction.move(moveOrigin, 1);
            check(!forward.equals(moveOrigin), "each locked movement direction changes position");
            check(direction.move(forward, -1).equals(moveOrigin), "backward movement reverses forward movement");
            var movedBounds = shape.at(forward);
            check(movedBounds.volume() == relocated.volume() && movedBounds.manaCost() == relocated.manaCost(),
                    "locked movement preserves shape volume and mana in every direction");
            check(movedBounds.min().subtract(relocated.min()).equals(forward.subtract(moveOrigin))
                    && movedBounds.max().subtract(relocated.max()).equals(forward.subtract(moveOrigin)),
                    "both locked boundaries translate equally");
        }
        var json = StaffSelectionShape.CODEC.encodeStart(JsonOps.INSTANCE, shape).getOrThrow();
        var restored = StaffSelectionShape.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        check(shape.equals(restored), "persistent record codec round trip");
        var bytes = Unpooled.buffer();
        try {
            StaffSelectionShape.STREAM_CODEC.encode(bytes, shape);
            check(shape.equals(StaffSelectionShape.STREAM_CODEC.decode(bytes)), "network record codec round trip");
        } finally { bytes.release(); }
        check(!new StaffSelectionShape(new BlockPos(-21, 0, 0)).fits(21), "saved negative offset enforces max side");
        var aimRange = StaffAimRange.of(4, 10);
        check(!aimRange.contains(2) && !aimRange.contains(12), "locked distances outside the interval are invalid");
        check(aimRange.contains(4) && aimRange.contains(7) && aimRange.contains(10), "interval includes its limits and air distances");
        check(aimRange.point(new Vec3(.5, 20.5, .5), new Vec3(0, 0, 1), 7).equals(new BlockPos(0, 20, 7)),
                "locked point uses the exact sight direction at a chosen air distance");
        var emptyRay = StaffAimRange.of(Double.NaN, 20);
        check(emptyRay.min() == 1 && emptyRay.max() == 20, "no block hit still gives a usable air interval");
        check(emptyRay.point(new Vec3(.5, 319.5, .5), new Vec3(0, 1, 0), 20).getY() == 339,
                "locked anchor calculation does not apply selection height limits");
        check(StaffAimRange.of(30, 10).min() == 10, "front distance never exceeds maximum reach");
        check(aimRange.withinMaximumReach(Vec3.ZERO, new Vec3(10, 0, 0), 0), "maximum center reach is inclusive");
        check(!aimRange.withinMaximumReach(Vec3.ZERO, new Vec3(10.25, 0, 0), 0),
                "client center remains inside strict reach");
        check(aimRange.withinMaximumReach(Vec3.ZERO, new Vec3(10.25, 0, 0), 1),
                "server accepts slight movement synchronization drift");
        check(!aimRange.withinMaximumReach(Vec3.ZERO, new Vec3(11.25, 0, 0), 1)
                && !aimRange.withinMaximumReach(Vec3.ZERO, new Vec3(Double.NaN, 0, 0), 1),
                "server still rejects distant and invalid centers");
        var cursor = new StaffAimCursor();
        Vec3 eye = new Vec3(.5, 20.5, .5);
        Vec3 south = new Vec3(0, 0, 1);
        Vec3 original = cursor.update(aimRange, eye, south, 7);
        cursor.move(aimRange, 4);
        check(cursor.update(aimRange, eye, south, 7).equals(original), "outward input keeps the last accepted distance");
        check(cursor.update(aimRange, eye.add(5, 3, 0), new Vec3(1, 0, 0), 7).equals(new Vec3(12.5, 23.5, .5)),
                "rejected outward input still allows camera following");
        cursor.move(aimRange, -2);
        Vec3 resumed = cursor.update(aimRange, eye, south, 7);
        check(resumed.equals(new Vec3(.5, 20.5, 5.5)), "reverse movement resumes from last valid distance without undoing overshoot");
        check(cursor.update(StaffAimRange.of(10, 12), eye, south, 7).equals(resumed),
                "changed front block lower bound freezes the last coordinate");
        cursor.move(aimRange, -6);
        check(cursor.update(aimRange, eye, south, 7).equals(resumed), "lower overflow retains the accepted distance");
        check(cursor.update(aimRange, eye, new Vec3(1, 0, 0), 7).equals(new Vec3(5.5, 20.5, .5)),
                "rejected backward input still allows camera following");
        cursor.move(aimRange, 2);
        check(cursor.update(aimRange, eye, south, 7).equals(new Vec3(.5, 20.5, 7.5)), "lower overflow resumes from last accepted distance");
        cursor.reset();
        check(cursor.update(aimRange, eye, south, 4).equals(new Vec3(.5, 20.5, 4.5)), "reset removes previous frozen coordinate and distance");
        for (int i = 0; i < 20; i++) {
            cursor.move(aimRange, -1);
            check(cursor.update(aimRange, eye, south, 4).equals(new Vec3(.5, 20.5, 4.5)), "repeated lower overflow preserves the coordinate");
        }
        check(cursor.update(aimRange, eye, new Vec3(1, 0, 0), 4).equals(new Vec3(4.5, 20.5, .5)),
                "minimum distance follows a changed view after repeated backward scrolls");
        check(cursor.update(aimRange, eye.add(2, 3, 1), south, 4).equals(new Vec3(2.5, 23.5, 5.5)),
                "minimum distance follows player movement after repeated backward scrolls");
        cursor.move(aimRange, 1);
        check(cursor.update(aimRange, eye, south, 4).equals(new Vec3(.5, 20.5, 5.5)), "one forward scroll works after repeated rejected backward scrolls");
        for (int i = 0; i < 5; i++) {
            cursor.move(aimRange, 1);
            cursor.update(aimRange, eye, south, 4);
        }
        for (int i = 0; i < 20; i++) {
            cursor.move(aimRange, 1);
            check(cursor.update(aimRange, eye, south, 4).equals(new Vec3(.5, 20.5, 10.5)), "repeated upper overflow preserves the coordinate");
        }
        check(cursor.update(aimRange, eye, new Vec3(1, 0, 0), 4).equals(new Vec3(10.5, 20.5, .5)),
                "maximum reach follows a changed view after repeated outward scrolls");
        cursor.move(aimRange, -1);
        check(cursor.update(aimRange, eye, south, 4).equals(new Vec3(.5, 20.5, 9.5)), "one backward scroll works after repeated rejected forward scrolls");
        cursor.reset();
        Vec3 beforeRangeChange = cursor.update(aimRange, eye, south, 5);
        var fartherBlock = StaffAimRange.of(8, 10);
        check(cursor.update(fartherBlock, eye, south, 5).equals(beforeRangeChange), "new farther front block freezes old coordinate");
        for (int i = 0; i < 2; i++) {
            cursor.move(fartherBlock, 1);
            check(cursor.update(fartherBlock, eye, south, 5).equals(beforeRangeChange), "forward inputs advance toward the changed interval while frozen");
        }
        cursor.move(fartherBlock, 1);
        check(cursor.update(fartherBlock, eye, south, 5).equals(new Vec3(.5, 20.5, 8.5)), "successive forward inputs recover after the lower bound moves");
        cursor.reset();
        var fractionalMinimum = StaffAimRange.of(4.25, 10);
        cursor.update(fractionalMinimum, eye, south, fractionalMinimum.min());
        cursor.move(fractionalMinimum, -1);
        check(cursor.update(fractionalMinimum, eye, new Vec3(1, 0, 0), fractionalMinimum.min())
                .equals(new Vec3(4.75, 20.5, .5)), "fractional block-hit lower bound follows the camera");
        cursor.move(fractionalMinimum, 1);
        check(cursor.update(fractionalMinimum, eye, south, fractionalMinimum.min()).equals(new Vec3(.5, 20.5, 5.75)),
                "one forward scroll leaves a fractional lower bound immediately");
        cursor.reset();
        Vec3 frozenCenter = cursor.update(aimRange, eye, south, 5);
        check(cursor.update(fartherBlock, eye, south, 5).equals(frozenCenter),
                "lower-bound change retains a frozen center while it is within reach");
        Vec3 movedEye = eye.add(20, 0, 0);
        Vec3 recoveredCenter = cursor.update(fartherBlock, movedEye, south, 5);
        check(recoveredCenter.equals(movedEye.add(0, 0, 8))
                && fartherBlock.withinMaximumReach(movedEye, recoveredCenter, 0),
                "walking away restores a frozen center inside current reach");
        check(cursor.update(fartherBlock, movedEye, new Vec3(1, 0, 0), 5).equals(movedEye.add(8, 0, 0)),
                "recovered center continues following the sight direction");
        System.out.println("Staff selection checks passed (18 directions, boundary adjustments, range and mana).");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

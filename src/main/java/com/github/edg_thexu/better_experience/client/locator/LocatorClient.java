package com.github.edg_thexu.better_experience.client.locator;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.gui.StructureLocatorScreen;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.StructureLocatorOrb;
import com.github.edg_thexu.better_experience.module.locator.LocatorNames;
import com.github.edg_thexu.better_experience.module.locator.StructureMarker;
import com.github.edg_thexu.better_experience.networks.s2c.LocatorCatalogPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Waypoint plugins are optional. Without either map the held orb still displays its saved target and bearing. */
public final class LocatorClient {
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    private static final Map<ResourceLocation, Pending> PENDING = new LinkedHashMap<>();
    private static final Map<ResourceLocation, StructureMarker> RECEIVED = new LinkedHashMap<>();
    private static ClientLevel level;
    private static Predicate<StructureMarker> journeyMap;
    private static boolean journeyMapFailed;
    private static final class Pending {
        final StructureMarker marker;
        boolean xaeroDone;
        boolean journeyDone;
        Pending(StructureMarker marker) { this.marker = marker; }
    }
    private LocatorClient() {}
    public static void installJourneyMap(Predicate<StructureMarker> sink) { journeyMap = sink; }

    private static boolean session() {
        var current = Minecraft.getInstance().level;
        if (level != current) {
            level = current;
            PENDING.clear();
            RECEIVED.clear();
            journeyMapFailed = false;
        }
        return current != null;
    }

    public static void open(LocatorCatalogPacket packet) {
        var mc = Minecraft.getInstance();
        if (!session() || mc.player == null || mc.player.getInventory().selected != packet.slot()
                || !(mc.player.getMainHandItem().getItem() instanceof StructureLocatorOrb)) return;
        mc.setScreen(new StructureLocatorScreen(packet));
    }

    public static void receive(List<StructureMarker> markers) {
        if (!session()) return;
        for (var marker : markers) {
            if (!marker.dimension().equals(level.dimension().location())) continue;
            if (RECEIVED.put(marker.waypointId(), marker) == null) PENDING.put(marker.waypointId(), new Pending(marker));
        }
    }

    public static void tick() {
        if (!session()) return;
        int processed = 0;
        for (var iterator = PENDING.values().iterator(); iterator.hasNext() && processed < 8; processed++) {
            var pending = iterator.next();
            if (!pending.xaeroDone) pending.xaeroDone = !ModList.get().isLoaded("xaerominimap") || XaeroLocatorWaypoints.add(pending.marker);
            if (!pending.journeyDone) {
                if (!ModList.get().isLoaded("journeymap") || journeyMapFailed) pending.journeyDone = true;
                else if (journeyMap != null) {
                    try { pending.journeyDone = journeyMap.test(pending.marker); }
                    catch (RuntimeException | LinkageError exception) {
                        journeyMapFailed = true;
                        pending.journeyDone = true;
                        Better_experience.LOGGER.warn("JourneyMap locator waypoint integration is unavailable", exception);
                    }
                }
            }
            if (pending.xaeroDone && pending.journeyDone) iterator.remove();
        }
    }

    public static void render(GuiGraphics graphics) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui
                || !(mc.player.getMainHandItem().getItem() instanceof StructureLocatorOrb)) return;
        var marker = mc.player.getMainHandItem().get(ModDataComponentTypes.LOCATOR_MARKER.get());
        if (marker == null || !marker.dimension().equals(mc.level.dimension().location())) return;
        double dx = marker.position().getX() - mc.player.getX(), dz = marker.position().getZ() - mc.player.getZ();
        double bearing = Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90 - mc.player.getYRot());
        int arrow = Math.floorMod((int) Math.round(bearing / 45), ARROWS.length);
        int y = graphics.guiHeight() - 70;
        String title = ARROWS[arrow] + " " + LocatorNames.structure(marker.structure()).getString() + " · " + (int) Math.hypot(dx, dz) + "m";
        graphics.drawCenteredString(mc.font, mc.font.plainSubstrByWidth(title, graphics.guiWidth() - 12), graphics.guiWidth() / 2, y, 0xA7E6FF);
        var point = marker.position();
        graphics.drawCenteredString(mc.font, point.getX() + ", " + point.getY() + ", " + point.getZ(), graphics.guiWidth() / 2, y + 11, 0xFFFFFF);
    }
}

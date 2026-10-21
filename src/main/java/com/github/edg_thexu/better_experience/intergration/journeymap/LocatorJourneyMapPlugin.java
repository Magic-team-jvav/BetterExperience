package com.github.edg_thexu.better_experience.intergration.journeymap;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.locator.LocatorClient;
import com.github.edg_thexu.better_experience.module.locator.LocatorNames;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.HashMap;
import java.util.Map;
import java.lang.ref.WeakReference;

/** Discovered by JourneyMap only when it is installed; its API is never bundled into this mod. */
@JourneyMapPlugin(apiVersion = "2.0.0")
public final class LocatorJourneyMapPlugin implements IClientPlugin {
    private WeakReference<ClientLevel> indexedLevel = new WeakReference<>(null);
    private final Map<String, String> waypointIds = new HashMap<>();
    @Override
    public void initialize(IClientAPI api) {
        LocatorClient.installJourneyMap(marker -> {
            if (api.getWorldId() == null || api.getDataPath(Better_experience.MODID) == null) return false;
            var level = Minecraft.getInstance().level;
            if (indexedLevel.get() != level) {
                indexedLevel = new WeakReference<>(level);
                waypointIds.clear();
                for (var saved : api.getWaypoints(Better_experience.MODID)) {
                    String id = saved.getCustomData("locator_id");
                    if (id != null) waypointIds.put(id, saved.getGuid());
                }
            }
            String id = marker.waypointId().toString();
            var guid = waypointIds.get(id);
            var waypoint = guid == null ? null : api.getWaypoint(Better_experience.MODID, guid);
            if (waypoint == null) waypoint = WaypointFactory.createWaypoint(Better_experience.MODID, marker.position(), marker.dimension().toString(), true);
            waypoint.setCustomData("locator_id", id);
            waypoint.setName(LocatorNames.structure(marker.structure()).getString());
            waypoint.setColor(0x5BD6FF);
            api.addWaypoint(Better_experience.MODID, waypoint);
            waypointIds.put(id, waypoint.getGuid());
            return true;
        });
    }

    @Override
    public String getModId() { return Better_experience.MODID; }
}

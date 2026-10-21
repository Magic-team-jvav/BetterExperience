package com.github.edg_thexu.better_experience.client.locator;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.module.locator.LocatorNames;
import com.github.edg_thexu.better_experience.module.locator.StructureMarker;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Isolates Xaero's public but versioned waypoint API, without a runtime dependency or private-field access. */
final class XaeroLocatorWaypoints {
    private static Method currentSession;
    private static Constructor<?> waypoint;
    private static boolean unavailable;
    private XaeroLocatorWaypoints() {}

    static boolean add(StructureMarker marker) {
        if (unavailable) return true;
        try {
            if (currentSession == null) {
                currentSession = Class.forName("xaero.common.XaeroMinimapSession").getMethod("getCurrentSession");
                waypoint = Class.forName("xaero.common.minimap.waypoints.Waypoint")
                        .getConstructor(int.class, int.class, int.class, String.class, String.class, int.class);
            }
            Object session = currentSession.invoke(null);
            if (session == null) return false;
            Object manager = invoke(session, "getWaypointsManager");
            if (manager == null) return false;
            Object world = invoke(manager, "getCurrentWorld");
            if (world == null || !marker.dimension().equals(Minecraft.getInstance().level.dimension().location())) return false;
            Object dimension = invoke(world, "getDimId");
            if (!Minecraft.getInstance().level.dimension().equals(dimension)) return false;
            Object set = invoke(world, "getCurrentSet");
            if (set == null) return false;
            var point = marker.position();
            String name = LocatorNames.structure(marker.structure()).getString() + " [" + marker.structure() + "]";
            var existing = (Iterable<?>) invoke(set, "getList");
            for (Object saved : existing) {
                if (invoke(saved, "getX").equals(point.getX()) && invoke(saved, "getZ").equals(point.getZ())
                        && ((String) invoke(saved, "getName")).endsWith("[" + marker.structure() + "]")) return true;
            }
            Object created = waypoint.newInstance(point.getX(), point.getY(), point.getZ(), name, "S", 11);
            Object io = invoke(manager, "getWorldManagerIO");
            if (io == null) return false;
            Method save = findAssignable(io.getClass(), "saveWorld", world.getClass());
            findAssignable(set.getClass(), "add", created.getClass()).invoke(set, created);
            save.invoke(io, world);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            unavailable = true;
            Better_experience.LOGGER.warn("Xaero locator waypoint integration is unavailable", exception);
            return true;
        }
    }

    private static Object invoke(Object receiver, String method) throws ReflectiveOperationException {
        return receiver.getClass().getMethod(method).invoke(receiver);
    }

    private static Method findAssignable(Class<?> type, String name, Class<?> argument) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(argument)) return method;
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }
}

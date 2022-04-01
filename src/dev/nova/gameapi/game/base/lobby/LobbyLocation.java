package dev.nova.gameapi.game.base.lobby;

import dev.nova.gameapi.game.base.instance.GameInstance;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public final class LobbyLocation implements ConfigurationSerializable {
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public LobbyLocation(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Location toBukkitLocation(Lobby lobby) {
        return new Location(lobby.getLobbyWorld(), x, y, z, yaw, pitch);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public static LobbyLocation deserialize(Map<String, Object> objectMap) {

        double x = 0, y = 0, z = 0;
        float pitch = 0, yaw = 0;

        if (objectMap.get("x") instanceof Integer) {
            x = (double) ((Integer) objectMap.get("x"));
        } else {
            x = (double) objectMap.get("x");
        }

        if (objectMap.get("y") instanceof Integer) {
            y = (double) ((Integer) objectMap.get("y"));
        } else {
            y = (double) objectMap.get("y");
        }

        if (objectMap.get("z") instanceof Integer) {
            z = (double) ((Integer) objectMap.get("z"));
        } else {
            z = (double) objectMap.get("z");
        }

        if (objectMap.get("yaw") instanceof Integer) {
            yaw = (float) ((Integer) objectMap.get("yaw"));
        } else {
            yaw = ((Double) objectMap.get("yaw")).floatValue();
        }

        if (objectMap.get("pitch") instanceof Integer) {
            pitch = (float) ((Integer) objectMap.get("pitch"));
        } else {
            pitch = ((Double) objectMap.get("pitch")).floatValue();
        }


        return new LobbyLocation(x, y, z, yaw, pitch);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("x", x);
        objectMap.put("y", y);
        objectMap.put("z", z);
        objectMap.put("yaw", yaw);
        objectMap.put("pitch", pitch);
        return objectMap;
    }
}

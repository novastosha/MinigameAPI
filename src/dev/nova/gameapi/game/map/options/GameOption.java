package dev.nova.gameapi.game.map.options;

import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.map.swm.GameLocation;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.Serializable;
import java.lang.reflect.Array;

public class GameOption {

    /**
     * Converts a location to be in the instance's map.
     *
     * @param location The location to convert.
     * @return The converted location.
     */
    public static Location convert(Location location, GameMap.Map map) {

        if(location == null || map == null) return location;
        if(location.getWorld() == map.getBukkitWorld()) return location;

        Location clone = location.clone();
        clone.setWorld(map.getBukkitWorld());

        return clone;
    }

    private final Object value;
    private final String key;
    private final OptionType type;
    private final GameMap.Map map;

    public GameOption(String optionKey, OptionType optionType, Object value, GameMap.Map map) {
        this.key = optionKey;
        this.type = optionType;
        this.value = value;
        this.map = map;
    }

    public GameMap.Map getMap() {
        return map;
    }

    public String getKey() {
        return key;
    }

    public OptionType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public boolean getAsBoolean() {
        return (boolean) value;
    }

    public String getAsString() {
        return (String) value;
    }

    public Location getAsLocation() {
        return getAsLocation(value);
    }

    private Location getAsLocation(Object value) {
        if (value instanceof GameLocation location) {
            return location.toBukkitLocation(map.getGameInstance());
        } else {
            return (Location) value;
        }
    }

    public ConfigurationSection getAsConfigurationSection() {
        return (ConfigurationSection) value;
    }

    public int getAsInt() {
        return (int) getValue();
    }

    @SuppressWarnings("unchecked")
    /**
     *
     * This method is not safe to use.
     *
     */
    public <T> T get() {
        return (T) getValue();
    }

    public double getAsDouble() {
        return (double) value;
    }

    public float getAsFloat() {
        return ((Double) value).floatValue();
    }

    public ItemStack getAsItemStack() {
        return get();
    }

    public Vector getAsVector() {
        return get();
    }

    @SuppressWarnings("unchecked")
    /**
     *
     *
     * This method was marked as unchecked, but it is safe to use.
     *
     */
    public <T> T[] getAsArray(Class<T> type) {

        ConfigurationSection valueConfig = (ConfigurationSection) getValue();

        T[] array = (T[]) Array.newInstance(type, valueConfig.getKeys(false).size());

        int iter = 0;
        for (String key : valueConfig.getKeys(false)) {
            try {
                T obj = null;
                try {
                    obj = (T) valueConfig.get(key);
                } catch (ClassCastException e) {
                    e.printStackTrace();
                    continue;
                }

                if (obj instanceof Location) {
                    obj = (T) getAsLocation(valueConfig.get(key));
                }

                array[iter] = obj;
                iter++;
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                break;
            }
        }

        return array;
    }


}

package dev.nova.gameapi.game.map.options;

import dev.nova.gameapi.game.map.GameMap;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.io.Serializable;
import java.lang.reflect.Array;

public class GameOption {

    private final Object value;
    private final String key;
    private final OptionType type;
    private final GameMap.Map map;


    public GameOption(String optionKey, OptionType optionType, Object value, GameMap.Map map) {
        this.key = optionKey;
        this.type = optionType;
        this.value = value;
        this.map = map;

        getAsArray(String.class);
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

    public Location getAsLocation() { return getAsLocation(value); };

    private Location getAsLocation(Object value) {
        Location location = (Location) value;

        return new Location(
                map.getBukkitWorld(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
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

                if(obj instanceof Location) {
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

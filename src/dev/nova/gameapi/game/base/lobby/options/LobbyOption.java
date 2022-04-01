package dev.nova.gameapi.game.base.lobby.options;

import dev.nova.gameapi.game.base.lobby.LobbyBase;
import dev.nova.gameapi.game.map.options.OptionType;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Array;

public class LobbyOption {

    private final Object value;
    private final String key;
    private final OptionType type;
    private final LobbyBase lobbyBase;

    public LobbyOption(String optionKey, OptionType optionType, Object value, LobbyBase lobbyBase) {
        this.key = optionKey;
        this.type = optionType;
        this.value = value;
        this.lobbyBase = lobbyBase;
    }

    public LobbyBase getLobbyBase() {return lobbyBase;}

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

    public ConfigurationSection getAsConfigurationSection() {
        return (ConfigurationSection) value;
    }

    public int getAsInt() {
        return (int) getValue();
    }

    @SuppressWarnings("unchecked")
    /**
     * This method is not safe to use.
     */
    public <T> T get() {
        return (T) getValue();
    }

    @SuppressWarnings("unchecked")
    /**
     * This method was marked as unchecked, but it is safe to use.
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
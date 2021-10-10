package dev.nova.gameapi.game.map.options;

import dev.nova.gameapi.game.map.GameMap;
import org.bukkit.Location;

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

    public boolean getAsBoolean(){
        return (boolean) value;
    }

    public String getAsString(){
        return (String) value;
    }

    public Location getAsLocation(){
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

}

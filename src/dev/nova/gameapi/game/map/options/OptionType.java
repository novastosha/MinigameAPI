package dev.nova.gameapi.game.map.options;

import dev.nova.gameapi.game.map.options.instance.GameOptionInfo;
public enum OptionType {

    BOOLEAN(null,"Boolean"),
    STRING(null,"String"),
    LOCATION(null,"Location"),
    INTEGER(null,"Integer"),
    ITEM(null,"Item"),
    VECTOR(null,"Vector"),
    DOUBLE(null,"Double"),
    FLOAT(null,"Float"),
    ARRAY(GameOptionInfo.OptionSpecificInfo.Array.class,"Array"),
    CONFIGURATION_SECTION(GameOptionInfo.OptionSpecificInfo.ConfigurationSection.class,"Configuration Section");

    public final Class<?> infoClass;
    public final String displayName;

    OptionType(Class<?> infoClass, String displayName) {
        this.displayName = displayName;
        this.infoClass = infoClass;
    }
}

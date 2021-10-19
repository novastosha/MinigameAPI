package dev.nova.gameapi.game.base.instance.values;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.values.base.GameValue;

public class EnumArrayGameValue<E extends Enum<E>> extends GameValue<E[]> {

    public EnumArrayGameValue(GameInstance instance, String id, String displayName, E[] defaultValue) {
        super(instance, id, displayName, defaultValue);
    }

    @Override
    public boolean autoSetValue() {
        return false;
    }
}

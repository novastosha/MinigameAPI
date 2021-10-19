package dev.nova.gameapi.game.base.instance.values;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.values.base.GameValue;

public class BooleanGameValue extends GameValue<Boolean> {
    public BooleanGameValue(GameInstance instance, String id, String displayName, Boolean defaultValue) {
        super(instance, id, displayName, defaultValue);
    }

    @Override
    public boolean autoSetValue() {
        setValue(!getValue());
        return true;
    }
}

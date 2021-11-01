package dev.nova.gameapi.game.base.instance.values;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.values.base.GameValue;

public class IntGameValue extends GameValue<Integer> {
    public IntGameValue(GameInstance instance, String id, String displayName, Integer defaultValue) {
        super(instance, id, displayName, defaultValue);
    }
}

package dev.nova.gameapi.game.base.values;

import dev.nova.gameapi.game.base.instance.GameInstance;

public class GameValueString extends GameValue<String>{
    public GameValueString(GameInstance instance, String codeName, String displayName) {
        super(instance, codeName, displayName);
    }

    @Override
    public String getValue() {
        return super.getValue();
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
    }
}

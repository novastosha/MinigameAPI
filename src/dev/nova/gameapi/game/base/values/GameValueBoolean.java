package dev.nova.gameapi.game.base.values;

import dev.nova.gameapi.game.base.instance.GameInstance;

public class GameValueBoolean extends GameValue<Boolean>{

    public GameValueBoolean(GameInstance instance, String codeName, String displayName) {
        super(instance, codeName, displayName);
    }

    @Override
    public Boolean getValue() {
        return super.getValue();
    }

    @Override
    public void setValue(Boolean value) {
        super.setValue(value);
    }


    @Override
    public boolean autoSetValue() {
        setValue(!getValue());
        super.autoSetValue();
        return true;
    }
}

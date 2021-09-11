package dev.nova.gameapi.game.base.values;

import dev.nova.gameapi.game.base.instance.GameInstance;

import javax.annotation.OverridingMethodsMustInvokeSuper;

public abstract class GameValue<T> {

    private final String displayName;
    private final String codeName;
    private final GameInstance instance;
    private T value;

    GameValue(GameInstance instance, String codeName, String displayName) {
        this.codeName = codeName;
        this.instance = instance;
        this.displayName = displayName;
    }

    public String getCodeName() {
        return codeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public T getValue(){
        return value;
    }

    @OverridingMethodsMustInvokeSuper
    public void setValue(T value) {
        this.value = value;
        instance.onValueUpdate(this);
    }

    @OverridingMethodsMustInvokeSuper
    public T autoSetValue(){
        instance.onValueUpdate(this);
        return value;
    }

}

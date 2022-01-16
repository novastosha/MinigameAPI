package dev.nova.gameapi.game.base.instance.values.base;

import dev.nova.gameapi.game.base.instance.GameInstance;


public abstract class GameValue<T> {

    private final T defaultValue;
    private final String id;
    private final GameInstance instance;
    private final String displayName;
    private T value;

    public GameValue(GameInstance instance, String id, String displayName, T defaultValue){
        this.id = id;
        this.displayName = displayName;
        this.instance = instance;
        this.value = defaultValue;
        this.defaultValue = defaultValue;

    }

    public T getValue() {
        return value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getId() {
        return id;
    }

    public void setValue(T value) {
        this.value = value;
        instance.onValueUpdate(this);
    }

    public boolean autoSetValue() {
        instance.onValueUpdate(this);
        return false;
    }
}

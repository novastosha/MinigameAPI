package dev.nova.gameapi.game.base;

import dev.nova.gameapi.game.base.instance.GameInstance;

public class GameEvent {

    private final int duration;
    private final String displayName;
    private final String codeName;
    private final GameInstance instance;

    public GameEvent(GameInstance instance, String codeName, String displayName, int duration){
        this.instance = instance;
        this.codeName = codeName;
        this.displayName = displayName;
        this.duration = duration;
    }

    public GameInstance getInstance() {
        return instance;
    }

    public int getDuration() {
        return duration;
    }

    public String getCodeName() {
        return codeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void injectEvent(){

    }

}

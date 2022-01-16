package dev.nova.gameapi.game.base.instance.events;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.events.exception.AlreadyInjectedException;

public abstract class GameEvent {

    public final int duration;
    public int durationLeft;
    public final String codeName;
    public final String displayName;
    protected GameInstance instance;

    public GameEvent(String codeName, String displayName, int duration){
        this.codeName = codeName;
        this.displayName = displayName;
        this.duration = duration;
        this.durationLeft = duration;
    }

    public void injectEvent(GameInstance instance) throws AlreadyInjectedException {
        if(this.instance == null){
            this.instance = instance;
        }else{
            throw new AlreadyInjectedException("Event: "+this.codeName+" is already injected to: "+this.instance.getGameID());
        }
    }

    public abstract void onEvent();

}

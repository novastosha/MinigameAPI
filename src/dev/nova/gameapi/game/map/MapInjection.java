package dev.nova.gameapi.game.map;

public abstract class MapInjection {

    protected final GameMap.Map map;

    public MapInjection(GameMap.Map map){
        this.map = map;
    }

    public abstract void onStart();
    public abstract void tick();
    public abstract void onEnd();

}

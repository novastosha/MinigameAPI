package dev.nova.gameapi.game.base.tasks;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.queue.Queue;

import java.util.ArrayList;

/**
 *
 * Simple class that handles game ticks.
 *
 */
public class GameTicker implements Runnable {
    private static GameTicker instance;

    public static GameTicker instance() {
        if(instance == null) instance = new GameTicker();
        return instance;
    }

    private GameTicker(){

    }

    @Override
    public void run() {

            for(Queue queue : Queue.CLOSED_QUEUES) {
                queue.coolDownTick();
            }

        for (GameBase base : GameManager.GAMES) {
            synchronized (this) {
                for(ArrayList<GameInstance> instances : base.getRunningInstances().values()) {

                    for (GameInstance instance : instances) {
                        instance.tick();

                        if(instance.getCurrentEvent() != null){
                            instance.getCurrentEvent().onTick();
                        }

                        assert instance.getGameMap() != null && instance.getMap() != null;

                        instance.getGameMap().onTick(instance.getMap());
                    }

                }
            }
        }
    }
}

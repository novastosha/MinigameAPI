package dev.nova.gameapi.game.base.tasks;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.scoreboard.Scoreboard;
import dev.nova.gameapi.game.manager.GameManager;

public class ScoreboardTicker implements Runnable {
    
    private static ScoreboardTicker instance;

    public static ScoreboardTicker instance() {
        if(instance == null) instance = new ScoreboardTicker();
        return instance;
    }

    private ScoreboardTicker(){

    }

    @Override
    public void run() {
        for(GameBase game : GameManager.GAMES){
            for(GameInstance instance : game.getRunningInstances()){
                Scoreboard scoreboard = instance.getScoreboard();

                if(scoreboard != null) {
                    if (scoreboard.getCycleColor() != null &&
                            scoreboard.getCyclePassedColor() != null) {
                        scoreboard.cycle();
                    }
                }
            }
        }
    }
}

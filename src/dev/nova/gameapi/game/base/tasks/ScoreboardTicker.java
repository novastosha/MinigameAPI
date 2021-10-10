package dev.nova.gameapi.game.base.tasks;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.scoreboard.Scoreboard;
import dev.nova.gameapi.game.base.scoreboard.player.PlayerScoreboard;
import dev.nova.gameapi.game.manager.GameManager;

import java.util.ArrayList;

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
            for(ArrayList<GameInstance> instances : game.getRunningInstances().values()) {

                for (GameInstance instance : instances) {
                    Scoreboard scoreboard = instance.getGameScoreboard();

                    if (scoreboard != null && instance.getPlayerScoreboards().isEmpty()) {
                        scoreboard.updateScoreboard();
                        continue;
                    }

                    if(!instance.getPlayerScoreboards().isEmpty()){
                        for(PlayerScoreboard scoreboard1 : instance.getPlayerScoreboards()){
                            scoreboard1.updateScoreboard();
                        }
                    }
                }
            }
        }
    }
}

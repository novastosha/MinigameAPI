package dev.nova.gameapi.game.base.tasks;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.manager.GameManager;

import java.util.ArrayList;

public class LobbyScoreboardTicker implements Runnable {
    
    private static LobbyScoreboardTicker instance;

    public static LobbyScoreboardTicker instance() {
        if(instance == null) instance = new LobbyScoreboardTicker();
        return instance;
    }

    private LobbyScoreboardTicker(){

    }

    @Override
    public void run() {
        for(GameBase game : GameManager.GAMES){
            if(game.hasLobbySupport()){
            }
        }
    }
}

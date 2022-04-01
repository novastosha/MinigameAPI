package dev.nova.gameapi.game.base.tasks;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.lobby.Lobby;
import dev.nova.gameapi.game.manager.GameManager;

import java.util.ArrayList;

public class LobbyTicker implements Runnable {
    
    private static LobbyTicker instance;

    public static LobbyTicker instance() {
        if(instance == null) instance = new LobbyTicker();
        return instance;
    }

    private LobbyTicker(){

    }

    @Override
    public void run() {
        for(GameBase game : GameManager.GAMES){
            if(game.hasLobbySupport()){
                for(Lobby lobby : game.getLobbyManager().getRunningLobbies()){
                    lobby.syncPlayers();
                    lobby.tick();
                }
            }
        }
    }
}

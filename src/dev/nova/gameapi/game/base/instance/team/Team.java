package dev.nova.gameapi.game.base.instance.team;

import dev.nova.gameapi.game.player.GamePlayer;

import java.util.ArrayList;

public class Team {

    private final String name;
    private final ArrayList<GamePlayer> players;

    public Team(String name){
        this.name = name;
        this.players = new ArrayList<GamePlayer>();
    }

    public String getName() {
        return name;
    }

    public ArrayList<GamePlayer> getPlayers() {
        return players;
    }
}

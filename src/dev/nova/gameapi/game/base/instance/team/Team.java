package dev.nova.gameapi.game.base.instance.team;

import dev.nova.gameapi.game.player.GamePlayer;

import java.util.ArrayList;

public class Team {

    private final String name;
    private final ArrayList<GamePlayer> players;
    private final int size;

    public Team(String name,int teamSize){
        this.name = name;
        this.size =  teamSize;
        this.players = new ArrayList<GamePlayer>();
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public void addPlayer(GamePlayer player){
        players.add(player);
        for(GamePlayer player1 : players){
            player1.getPlayer().sendMessage("§6"+player.getPlayer().getName()+"§b joined your team! §6("+players.size()+"/"+size+")");
        }
    }

    public ArrayList<GamePlayer> getPlayers() {
        return players;
    }
}

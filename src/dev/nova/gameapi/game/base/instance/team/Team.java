package dev.nova.gameapi.game.base.instance.team;

import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class Team {

    private final String name;
    private final int teamSize;
    private final ChatColor color;
    private final ArrayList<GamePlayer> players;

    public Team(String name, int teamSize, ChatColor color,GamePlayer[] players) {
        if (color.isFormat()) {
            color = ChatColor.WHITE;
        }
        this.name = name;
        this.teamSize = teamSize;
        this.color = color;
        this.players = new ArrayList<GamePlayer>();
        this.players.addAll(List.of(players));
    }

    public Team(String name, int teamSize,ChatColor color){
        this(name,teamSize,color,new GamePlayer[0]);
    }

    public Team(String name, int teamSize) {
        this(name, teamSize, ChatColor.WHITE,new GamePlayer[0]);
    }

    public Team(String name, int teamSize,GamePlayer[] players) {
        this(name, teamSize, ChatColor.WHITE,players);
    }

    public ArrayList<GamePlayer> getPlayers() {
        return players;
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {
        return color;
    }

    public int getTeamSize() {
        return teamSize;
    }


    public void addPlayer(GamePlayer partyPlayer) {
        players.add(partyPlayer);
        for(GamePlayer player : players){
            player.getPlayer().sendMessage("§7"+partyPlayer.getPlayer().getName()+" has joined your team!");
        }
    }

    public void removePlayer(GamePlayer player,boolean silent) {
        players.remove(player);
        for(GamePlayer player1 : players){
            player1.getPlayer().sendMessage("§7"+player.getPlayer().getName()+" has left your team!");
        }
    }
}

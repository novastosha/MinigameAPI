package dev.nova.gameapi.game.base.instance.team;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.controller.GameController;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.ChatColor;
;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

public abstract class TeamGameInstance extends GameInstance {


    protected final ArrayList<Team> teams;

    protected Function<Team, String> teamEliminationFormat = (team -> ChatColor.GRAY+"Team "+team.getColor()+"§l"+team.getName().toUpperCase()+ChatColor.GRAY+" has been eliminated!");

    public TeamGameInstance(String displayName,  String gameBase, GameMap map, String[] gameDescription, String gameBrief, boolean canBeSpectated, GameController[] gameControllers) {
        super(displayName, gameBase, map, gameDescription, gameBrief, canBeSpectated, gameControllers);
        this.teams = new ArrayList<>();
        playersChatFormat = (message -> getTeam(message.player()).getColor()+"["+getTeam(message.player()).getName().toUpperCase()+"] "+message.player().getPlayer().getName()
                + ChatColor.RESET+": "+message.message());
    }
    
    protected void addTeam(Team team){
        teams.add(team);
    }
    protected void addTeams(Team team, Team... teams){
        this.teams.add(team);
        this.teams.addAll(Arrays.asList(teams));
    }

    public Team getTeam(GamePlayer player) {
        for(Team team : teams){
            if(team.getPlayers().contains(player)) return team;
        }
        return null;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void onStart() {

        for(GamePlayer player : players) {
            if(getTeam(player) == null) {
                for (Team team : teams) {

                    if (team.getPlayers().size() + 1 <= team.getTeamSize()) {
                        team.addPlayer(player);
                        break;
                    }
                }
            }
        }

        ArrayList<Team> toClear = new ArrayList<>();
        for(Team team : teams){
            if(team.getPlayers().size() == 0){
                toClear.add(team);
                getUtils().sendMessage(teamEliminationFormat.apply(team));
            }
        }

        for(Team team : toClear){
            teams.remove(team);
        }

        toClear.clear();

        super.onStart();
    }

    @Override
    public void switchToSpectator(GamePlayer player) {
        super.switchToSpectator(player);
        getTeam(player).removePlayer(player,true);
    }

    public ArrayList<Team> getTeams() {
        return teams;
    }


    @Override
    public void onEnd() {
        super.onEnd();
        teams.clear();
    }
}

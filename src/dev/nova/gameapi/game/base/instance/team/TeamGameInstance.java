package dev.nova.gameapi.game.base.instance.team;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.controller.GameController;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class TeamGameInstance extends GameInstance {

    private final int teamSize;
    private final List<Team> teams;
    private final boolean preSetTeams;
    private int ticks;


    public TeamGameInstance(String displayName, @Nonnull String gameBase, GameMap map, String[] gameDescription, String gameBrief,int teamSize,boolean spectatable, GameController[] gameControllers) {
        this(displayName,gameBase,map,gameDescription,gameBrief,new Team[0],teamSize,spectatable,gameControllers);
    }

    public TeamGameInstance(String displayName, @Nonnull String gameBase, GameMap map, String[] gameDescription, String gameBrief, Team[] preSetTeams, int teamSize, boolean spectatable, GameController[] gameControllers){
        super(displayName, gameBase, map, gameDescription, gameBrief,spectatable,gameControllers);
        this.teamSize = teamSize;
        this.teams = Arrays.asList(preSetTeams);
        this.preSetTeams = true;

        if(preSetTeams.length == 0 || teamSize == 0){
            throw new IllegalArgumentException("[preSetTeams="+preSetTeams.length+",teamSize="+teamSize+"]");
        }
    }

    @Override
    public void onStart() {

        getUtils().sendTitle("§bGenerating teams!","§7Please wait...",10,10,20);

        if(!preSetTeams) {

            int team = 0;
            int teamGlobal = 0;

            ArrayList<GamePlayer> players = new ArrayList<>();

            for (GamePlayer player : getPlayers()) {

                teamGlobal++;

                players.add(player);

                if (team == teamSize) {
                    Team teamA = new Team(String.valueOf(teamGlobal), teamSize);
                    teams.add(teamA);

                    for (GamePlayer player1 : players) {
                        teamA.addPlayer(player1);
                    }

                    players.clear();
                    team = 0;
                }

                team++;
            }

            if (players.size() != 0) {

                Team finalT = new Team("FI" + players.size(), players.size());

                for (GamePlayer player : players) {
                    finalT.addPlayer(player);
                }

                teams.add(finalT);
            }
        }else{

            int teamIndex = 0;

            ArrayList<GamePlayer> players = new ArrayList<>();

            for(Team team : getTeams()){
                for(GamePlayer player : getPlayers()){

                    players.add(player);

                    if (teamIndex == teamSize) {

                        for (GamePlayer player1 : players) {
                            team.addPlayer(player1);
                        }

                        players.clear();
                        teamIndex = 0;
                    }

                    teamIndex++;
                }
            }

            if (players.size() != 0) {

                Team last = getTeams().get(getTeams().size()-1);

                for(GamePlayer player : players){
                    last.addPlayer(player);
                }
            }
        }

        super.onStart();
    }

    public List<Team> getTeams() {
        return teams;
    }

    public int getTeamSize() {
        return teamSize;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void tick() {

        ticks++;

        if(ticks == 20L) {
            ArrayList<Team> toEliminate = new ArrayList<>();
            for (Team team : teams) {
                if (team.getPlayers().size() == 0) {
                    getUtils().sendMessage("§bTeam: §6" + team.getName() + "§b has been eliminated!");
                    toEliminate.add(team);
                }
            }

            for (Team team : toEliminate) {
                teams.remove(team);
            }

            toEliminate.clear();
        }
    }

    public Team getTeamOf(GamePlayer player){
        if(player.isInGame() && player.getGame() == this){
            for(Team team : teams){
                if(team.getPlayers().contains(player)) return team;
            }
        }
        return null;
    }
}

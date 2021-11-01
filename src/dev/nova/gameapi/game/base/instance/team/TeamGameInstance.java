package dev.nova.gameapi.game.base.instance.team;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.controller.GameController;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class TeamGameInstance extends GameInstance {


    protected final ArrayList<Team> teams;

    public TeamGameInstance(String displayName, @NotNull String gameBase, GameMap map, String[] gameDescription, String gameBrief, boolean canBeSpectated, GameController[] gameControllers) {
        super(displayName, gameBase, map, gameDescription, gameBrief, canBeSpectated, gameControllers);
        this.teams = new ArrayList<>();
    }
    
    protected void addTeam(Team team){
        teams.add(team);
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
        super.onStart();
    }
}

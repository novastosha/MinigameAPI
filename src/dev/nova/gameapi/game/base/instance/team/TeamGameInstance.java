package dev.nova.gameapi.game.base.instance.team;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.map.GameMap;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public abstract class TeamGameInstance extends GameInstance {

    private final int teamSize;
    private final ArrayList<Team> teams;


    public TeamGameInstance(String displayName, @Nonnull String gameBase, GameMap map, String[] gameDescription, String gameBrief,int teamSize) {
        super(displayName, gameBase, map, gameDescription, gameBrief);
        this.teamSize = teamSize;
        this.teams = new ArrayList<Team>();
    }


    public ArrayList<Team> getTeams() {
        return teams;
    }

    public int getTeamSize() {
        return teamSize;
    }
}

package dev.nova.gameapi.game.base.instance.team;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.controller.GameController;
import dev.nova.gameapi.game.base.instance.formats.chat.Message;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.party.Party;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

public abstract class TeamGameInstance extends GameInstance {


    protected final ArrayList<Team> teams;

    protected Function<Team, String> teamEliminationFormat = (team -> ChatColor.GRAY + "Team " + team.getColor() + "§l" + team.getName().toUpperCase() + ChatColor.GRAY + " has been eliminated!");
    protected Function<Message, String> teamChatFormat = (message -> ChatColor.GRAY + "[TEAM CHAT] " + getTeam(message.player()).getColor() + message.player().getPlayer().getName() + ChatColor.WHITE + ": " + message.message());

    public TeamGameInstance(String displayName, String gameBase, GameMap map, String[] gameDescription, String gameBrief, boolean canBeSpectated, GameController[] gameControllers,boolean rejoinCapable) {
        super(displayName, gameBase, map, gameDescription, gameBrief, canBeSpectated, gameControllers,rejoinCapable);
        this.teams = new ArrayList<>();
        playersChatFormat = (message -> getTeam(message.player()).getColor() + "[" + getTeam(message.player()).getName().toUpperCase() + "] " + message.player().getPlayer().getName()
                + ChatColor.RESET + ": " + message.message());

        disconnectionFormat = (player -> (getTeam(player) == null ? ChatColor.GRAY : getTeam(player).getColor())+player.getPlayer().getName()+ChatColor.GRAY+" disconnected.");
        rejoiningFormat = (player -> (getTeam(player) == null ? ChatColor.GRAY : getTeam(player).getColor())+player.getPlayer().getName()+ChatColor.GRAY+" reconnected.");
    }


    protected void addTeam(Team team) {
        teams.add(team);
    }

    protected void addTeams(Team team, Team... teams) {
        this.teams.add(team);
        this.teams.addAll(Arrays.asList(teams));
    }

    public Team getTeam(GamePlayer player) {
        for (Team team : teams) {
            if (team.getPlayers().contains(player)) return team;
        }
        return null;
    }

    @Override
    public void handleChatEvent(PlayerChatEvent event) {

        GamePlayer player = GamePlayer.getPlayer(event.getPlayer());

        event.setCancelled(true);

        String message = teamChatFormat.apply(new Message(player, event.getMessage()));

        for (GamePlayer teamPlayer : getTeam(player).getPlayers()) {
            teamPlayer.getPlayer().sendMessage(message);
        }
    }

    @Override
    public void onStart() {

        ArrayList<GamePlayer> playersA = new ArrayList<>();
        ArrayList<Party> parties = new ArrayList<>();
        for (GamePlayer player : players) {
            if (playersA.contains(player)) continue;
            if (!player.isInParty()) {
                if (getTeam(player) == null) {
                    for (Team team : teams) {
                        if (team.getPlayers().size() + 1 <= team.getTeamSize()) {
                            team.addPlayer(player);
                            playersA.add(player);
                            break;
                        }
                    }
                }
            } else {
                if (parties.contains(player.getParty())) continue;
                for (GamePlayer partyPlayer : player.getParty().getPartyMembers().keySet()) {
                    for (Team team : teams) {
                        if (team.getPlayers().size() + 1 <= team.getTeamSize()) {
                            team.addPlayer(partyPlayer);
                            break;
                        }
                    }
                }
                parties.add(player.getParty());
            }
        }

        playersA.clear();
        parties.clear();

        ArrayList<Team> toClear = new ArrayList<>();
        for (Team team : teams) {
            if (team.getPlayers().size() == 0) {
                toClear.add(team);
                eliminate(team);
            }
        }

        for(Team to : toClear){
            teams.remove(to);
        }

        toClear.clear();

        super.onStart();
    }

    /**
     *
     * REMOVING TEAM IS NOT DONE HERE due to chance of {@link java.util.ConcurrentModificationException} being thrown!
     *
     * @param team The team to eliminate
     */
    public void eliminate(Team team) {
        getUtils().sendMessage(teamEliminationFormat.apply(team));

    }

    @Override
    public void switchToSpectator(GamePlayer player) {
        super.switchToSpectator(player);
        getTeam(player).removePlayer(player, true);
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

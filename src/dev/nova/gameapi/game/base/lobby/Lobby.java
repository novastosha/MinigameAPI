package dev.nova.gameapi.game.base.lobby;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public abstract class Lobby {

    private final GameBase.LobbyManager manager;
    private final World lobbyWorld;
    private final LobbyBase base;
    private final ArrayList<GamePlayer> players;

    public Lobby(GameBase.LobbyManager manager,LobbyBase base, World lobbyWorld) {
        this.manager = manager;
        this.base = base;
        this.lobbyWorld = lobbyWorld;
        this.players = new ArrayList<GamePlayer>();
    }

    public LobbyBase getBase() {
        return base;
    }

    public World getLobbyWorld() {
        return lobbyWorld;
    }

    public GameBase.LobbyManager getManager() {
        return manager;
    }

    public void onJoin(GamePlayer player) {
        players.add(player);
        player.getPlayer().teleport(base.getSpawn(this));
        GameInstance.normalReset(player, GameMode.ADVENTURE);
    }

    public abstract void tick();

    public void onLeave(GamePlayer player) {

    }

    public void syncPlayers() {
        for(Player player : lobbyWorld.getPlayers()) {
            GamePlayer asGamePlayer = GamePlayer.getPlayer(player);

            if(!players.contains(asGamePlayer)) {
                onJoin(asGamePlayer);
            }
        }
    }

    public void close() {
        syncPlayers();
        for(GamePlayer player : players) {
            if(!base.getManager().getGame().moveToLobby(player)) {
                player.getPlayer().kickPlayer(ChatColor.RED+"Lobby is closing!");
            }
        }
        Bukkit.getServer().unloadWorld(lobbyWorld,false);
    }

    public ArrayList<GamePlayer> getPlayers() {
        return players;
    }
}

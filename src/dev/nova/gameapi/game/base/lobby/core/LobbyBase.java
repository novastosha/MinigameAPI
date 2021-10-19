package dev.nova.gameapi.game.base.lobby.core;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.lobby.Lobby;
import org.bukkit.Location;
import org.bukkit.World;

public record LobbyBase(GameBase game, World bukkitWorld,String[] spawnData){
    public Location parseLocation(Lobby lobby) {
        return new Location(lobby.getWorld(),
                Double.parseDouble(spawnData[0]),
                Double.parseDouble(spawnData[1]),
                Double.parseDouble(spawnData[2]),
                Float.parseFloat(spawnData[3]),
                Float.parseFloat(spawnData[4])
        );
    }
}

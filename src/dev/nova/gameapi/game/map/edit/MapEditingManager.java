package dev.nova.gameapi.game.map.edit;

import com.grinderwolf.swm.api.world.SlimeWorld;
import dev.nova.gameapi.GameAPI;
import dev.nova.gameapi.game.base.lobby.LobbyBase;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.map.Map;
import dev.nova.gameapi.game.map.edit.map.EditingMap;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class MapEditingManager {

    private static final AtomicInteger ID = new AtomicInteger(0);

    private final HashMap<GamePlayer, EditingMap> maps;

    public MapEditingManager() {
        this.maps = new HashMap<>();
    }

    public void editMap(GamePlayer player, Map map) {
        if(maps.containsKey(player)){
            player.getPlayer().sendMessage(ChatColor.RED+"You are already editing an other map: "+maps.get(player).getMap().getDisplayName());
            player.getPlayer().sendMessage(ChatColor.RED+"If you believe this is an error, use /map forceExit");
            return;
        }

        if(isMapBeingEdited(map)) {
            player.getPlayer().sendMessage(ChatColor.RED+"This map already being edited by someone else!");
            if(map instanceof GameMap gameMap) {
                player.getPlayer().sendMessage(ChatColor.RED + "If you believe this is an error, use /map forceStop " + map.getGameBase().getCodeName() + " " + gameMap.getInstanceCode() + " " + map.getCodeName());
            }else{
                player.getPlayer().sendMessage(ChatColor.RED + "If you believe this is an error, use /map " + map.getGameBase().getCodeName() + " "  + map.getCodeName()+" stop");
            }

            //The system for map editing is now: 1 map 1 editor; however this will be removed if I find a way to save cloned worlds.
            return;
        }

        SlimeWorld slimeWorld = map instanceof GameMap ? GameManager.SLIME_WORLD_MAPS.get(map) : map.getGameBase().getLobbyManager().SLIME_WORLD_MAPS.get(((LobbyBase) map));
        GameAPI.getSlime().generateWorld(slimeWorld);

        maps.put(player,new EditingMap(player, Bukkit.getWorld(slimeWorld.getName()),map,slimeWorld));
        maps.get(player).enter();
    }

    public HashMap<GamePlayer, EditingMap> getCurrentlyEditingMaps() {
        return maps;
    }

    public boolean isMapBeingEdited(Map map) {
        return Arrays.stream(maps.values().toArray(new EditingMap[0])).anyMatch(editingMap -> editingMap.getMap() == map);
    }

    public void forceStop(Player bukkitPlayer, Map map) {
        if(!isMapBeingEdited(map)){
            return;
        }

        bukkitPlayer.sendMessage(ChatColor.GREEN+"Closing editing session of map: "+map.getDisplayName());

        EditingMap editingMapA = Arrays.stream(maps.values().toArray(new EditingMap[0])).filter(editingMap -> editingMap.getMap() == map).findFirst().get();

        editingMapA.getEditor().getPlayer().sendMessage(ChatColor.RED+"Closing your editing session of: "+map.getDisplayName()+" forcefully by: "+bukkitPlayer.getName());
        editingMapA.close(false);
    }
}

package dev.nova.gameapi.game.base.tasks;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.GameState;
import dev.nova.gameapi.game.base.instance.eventListener.GlobalEventListener;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class GameEventListener extends GlobalEventListener {

    public GameEventListener(JavaPlugin plugin, boolean ignoreCancelled) {
        super(plugin, ignoreCancelled);
    }

    @Override
    protected void onEvent(Event event) {
        if (event instanceof ChunkLoadEvent || event instanceof ChunkUnloadEvent) {
            return;
        }

        for (GameBase base : GameManager.GAMES) {
            for (ArrayList<GameInstance> instances : base.getRunningInstances().values()) {
                for (GameInstance instance : instances) {
                    if (!instance.getGameState().equals(GameState.ENDED) && !instance.getGameState().equals(GameState.CLOSED) && !instance.getGameState().equals(GameState.NOT_STARTED)) {
                        if (event instanceof PlayerDeathEvent) {
                            PlayerDeathEvent deathEvent = (PlayerDeathEvent) event;

                            GamePlayer victim = GamePlayer.getPlayer(deathEvent.getEntity());

                            if (deathEvent.getEntity().getKiller() == null) {
                                if (instance.getPlayers().contains(victim)) {
                                    callEvent(instance,event);
                                }
                            } else {
                                GamePlayer killer = GamePlayer.getPlayer(deathEvent.getEntity().getKiller());
                                if (instance.getPlayers().contains(victim) && instance.getPlayers().contains(killer)) {
                                    callEvent(instance,event);

                                }
                            }
                            continue;
                        }

                        if(event instanceof InventoryClickEvent clickEvent) {
                            ArrayList<HumanEntity> players = new ArrayList<>();

                            for(GamePlayer p : instance.getPlayers()){
                                players.add(p.getPlayer());
                            }

                            if(players.containsAll(clickEvent.getViewers())){
                                callEvent(instance,clickEvent);
                            }
                            continue;
                        }

                        try {

                            Inventory inventory = (Inventory) event.getClass().getMethod("getInventory").invoke(event);

                            if(inventory == null){
                                inventory = (Inventory) event.getClass().getMethod("getClickedInventory").invoke(event);
                            }

                            if(inventory != null) {
                                ArrayList<HumanEntity> players = new ArrayList<>();

                                for(GamePlayer p : instance.getPlayers()){
                                    players.add(p.getPlayer());
                                }

                                if(players.containsAll(inventory.getViewers())){
                                    callEvent(instance,event);
                                    continue;
                                }
                            }

                            World world = (World) event.getClass().getMethod("getWorld").invoke(event);

                            if (world != null) {
                                if (instance.getMap() != null && world == instance.getMap().getBukkitWorld()) {
                                    callEvent(instance,event);
                                    continue;
                                }
                            }
                            Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);

                            if (player != null) {
                                GamePlayer gamePlayer = GamePlayer.getPlayer(player);

                                if (instance.getPlayers().contains(gamePlayer)) {
                                    callEvent(instance,event);
                                    continue;
                                }
                            }

                            Entity entity = (Entity) event.getClass().getMethod("getEntity").invoke(event);

                            if (entity != null) {
                                if (!(entity instanceof Player)) {
                                    if (instance.getMap() != null && instance.getMap().getBukkitWorld().getEntities().contains(entity)) {
                                        callEvent(instance,event);
                                        continue;
                                    }
                                } else {
                                    GamePlayer player1 = GamePlayer.getPlayer((Player) entity);
                                    if (instance.getPlayers().contains(player1)) {
                                        callEvent(instance,event);
                                    }
                                }
                            }
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
                            callEvent(instance,event);
                        }
                    }
                }
            }
        }
    }

    private void callEvent(GameInstance instance, Event event) {
        if(instance.getGameState().equals(GameState.AFTER_END)){
            instance.afterEnd(event);
        }else{
            instance.onEvent(event);
        }
    }
}

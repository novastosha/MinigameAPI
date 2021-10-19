package dev.nova.gameapi.game.base.tasks;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.GameState;
import dev.nova.gameapi.game.base.instance.eventListener.GlobalEventListener;
import dev.nova.gameapi.game.base.scoreboard.player.PlayerScoreboardLine;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.function.Consumer;

public class GameEventListener extends GlobalEventListener {

    public GameEventListener(JavaPlugin plugin, boolean ignoreCancelled) {
        super(plugin, ignoreCancelled);
    }

    @Override
    protected void onEvent(Event event) {
        if (event instanceof ChunkLoadEvent || event instanceof ChunkUnloadEvent || event instanceof EntityAirChangeEvent) {
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
                                    instance.onEvent(event);
                                }
                            } else {
                                GamePlayer killer = GamePlayer.getPlayer(deathEvent.getEntity().getKiller());
                                if (instance.getPlayers().contains(victim) && instance.getPlayers().contains(killer)) {
                                    instance.onEvent(event);

                                }
                            }
                            continue;
                        }

                        try {
                            World world = (World) event.getClass().getMethod("getWorld").invoke(event);

                            if (world != null) {
                                if (instance.getMap() != null && world == instance.getMap().getBukkitWorld()) {
                                    instance.onEvent(event);
                                    continue;
                                }
                            }
                            Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);

                            if (player != null) {
                                GamePlayer gamePlayer = GamePlayer.getPlayer(player);

                                if (instance.getPlayers().contains(gamePlayer)) {
                                    instance.onEvent(event);
                                    continue;
                                }
                            }

                            Entity entity = (Entity) event.getClass().getMethod("getEntity").invoke(event);

                            if (entity != null) {
                                if (!(entity instanceof Player)) {
                                    if (instance.getMap() != null && instance.getMap().getBukkitWorld().getEntities().contains(entity)) {
                                        instance.onEvent(event);
                                        continue;
                                    }
                                } else {
                                    GamePlayer player1 = GamePlayer.getPlayer((Player) entity);
                                    if (instance.getPlayers().contains(player1)) {
                                        instance.onEvent(event);
                                    }
                                }
                            }
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
                            instance.onEvent(event);
                        }
                    }
                }
            }
        }
    }
}

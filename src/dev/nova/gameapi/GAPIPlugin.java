/**
 * Copyright (c) 2021 novastosha
 */
package dev.nova.gameapi;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.commands.*;
import dev.nova.gameapi.game.base.lobby.Lobby;
import dev.nova.gameapi.game.base.mysql.DatabaseConnection;
import dev.nova.gameapi.game.base.tasks.GameEventListener;
import dev.nova.gameapi.game.base.tasks.GameTicker;
import dev.nova.gameapi.game.base.tasks.LobbyScoreboardTicker;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.game.queue.command.ForceStartCommand;
import dev.nova.gameapi.game.queue.command.QueueCommand;
import dev.nova.gameapi.party.Party;
import dev.nova.gameapi.party.command.PartyCommand;
import dev.nova.gameapi.game.base.listener.ChatListener;
import dev.nova.gameapi.party.poll.Poll;
import dev.nova.gameapi.party.poll.PollAnswer;
import dev.nova.gameapi.utils.Files;
import dev.nova.gameapi.utils.api.schematics.commands.SchematicsSaveCommand;
import dev.nova.gameapi.utils.api.schematics.commands.SchematicsWandCommand;
import dev.nova.gameapi.utils.api.schematics.commands.listener.SchematicsInteractEventListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

public class GAPIPlugin extends JavaPlugin {

    private static Connection coinsConnection;

    public static Connection getCoinsConnection() {
        return coinsConnection;
    }

    public static boolean isCoinsSystemHooked() {
        return coinsConnection != null;
    }

    @Override
    public void onEnable() {

        new GameEventListener(this, true);
        Bukkit.getServer().getPluginManager().registerEvents(new ChatListener(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new SchematicsInteractEventListener(), this);

        QueueCommand queueCommand = new QueueCommand();
        getCommand("queue").setExecutor(queueCommand);
        getCommand("queue").setTabCompleter(queueCommand);

        getCommand("leave").setExecutor(new LeaveCommand());

        getCommand("revive").setExecutor(new ReviveCommand());
        getCommand("spectator").setExecutor(new SpectatorCommand());

        SpectateCommand spectateCommand = new SpectateCommand();
        getCommand("spectate").setExecutor(spectateCommand);
        getCommand("spectate").setTabCompleter(spectateCommand);

        PartyCommand partyCommand = new PartyCommand();
        getCommand("party").setExecutor(partyCommand);

        getCommand("wand").setExecutor(new SchematicsWandCommand());
        getCommand("saveSchematic").setExecutor(new SchematicsSaveCommand());

        getCommand("forcestart").setExecutor(new ForceStartCommand());

        getCommand("rejoin").setExecutor(new RejoinCommand());

        for (Files files : Files.values()) {
            if (files.isToCreate()) {
                if (!files.getFile().exists()) {
                    if (!files.isFile()) {
                        files.getFile().mkdirs();
                    } else {
                        try {
                            files.getFile().createNewFile();
                        } catch (IOException ignored) {

                        }

                    }
                    files.onCreate();
                }
            }
        }


        if (getConfig().getBoolean("coins-support.enabled")) {
            Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Hooking coins system...");

            String host = getConfig().getString("coins-support.host"), username = getConfig().getString("coins-support.username"),
                    password = getConfig().getString("coins-support.password");

            int port = getConfig().getInt("coins-support.port");

            this.coinsConnection = new DatabaseConnection(host, port, username, password, "coins", "coins").connection;
        }

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, GameTicker.instance(), 0L, 0L);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, LobbyScoreboardTicker.instance(), 0L, 0L);
        ArrayList<Party> parties = new ArrayList<>();
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (Player playerB : Bukkit.getServer().getOnlinePlayers()) {
                    GamePlayer player = GamePlayer.getPlayer(playerB);

                    if (!player.isInParty()) continue;

                    if (parties.contains(player.getParty())) {
                        continue;
                    }

                    for (Map.Entry<Poll, Integer> entry : player.getParty().getRunningPolls().entrySet()) {

                        if (entry.getKey().hasEnded()) {
                            continue;
                        }

                        int left = entry.getValue() - 1;

                        if (left == 0) {
                            entry.getKey().end();
                        }

                        player.getParty().getRunningPolls().put(entry.getKey(),left);
                    }

                    parties.add(player.getParty());
                }
                parties.clear();
            }
        }, 00L, 20L);

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, ListenerPriority.HIGHEST, PacketType.Play.Client.UPDATE_SIGN) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player bukkitPlayer = event.getPlayer();
                GamePlayer player = GamePlayer.getPlayer(bukkitPlayer);

                if (!player.isInParty()) {
                    bukkitPlayer.sendBlockChange(new Location(bukkitPlayer.getWorld(), 0, 0, 0), bukkitPlayer.getWorld().getBlockData(0, 0, 0));
                    return;
                }

                if (player.getParty().getLeader() != player) {
                    bukkitPlayer.sendBlockChange(new Location(bukkitPlayer.getWorld(), 0, 0, 0), bukkitPlayer.getWorld().getBlockData(0, 0, 0));
                    return;
                }

                if (!player.getParty().isOnPollCreation()) {
                    bukkitPlayer.sendBlockChange(new Location(bukkitPlayer.getWorld(), 0, 0, 0), bukkitPlayer.getWorld().getBlockData(0, 0, 0));
                    return;
                }

                StringBuilder builder = new StringBuilder();

                int i = 0;
                for (String line : event.getPacket().getStringArrays().getValues().get(0)) {

                    if(line.isBlank() || line.isEmpty()) continue;

                    builder.append(i == 0 ? "" : " ").append(line);
                    i++;
                }

                String built = builder.toString();

                if (player.getParty().data == null) {
                    if (built.isBlank() || built.isEmpty()) {
                        bukkitPlayer.sendBlockChange(new Location(bukkitPlayer.getWorld(), 0, 0, 0), bukkitPlayer.getWorld().getBlockData(0, 0, 0));
                        player.getParty().handleError(Party.Error.EMPTY_POLL_QUESTION);
                        return;
                    }

                    Bukkit.getServer().getScheduler().runTask(GAPIPlugin.this, new Runnable() {
                        @Override
                        public void run() {
                            bukkitPlayer.sendBlockChange(new Location(bukkitPlayer.getWorld(), 0, 0, 0), bukkitPlayer.getWorld().getBlockData(0, 0, 0));
                            player.getParty().startCreation(builder.toString());
                        }
                    });
                } else {
                    if (built.isBlank() || built.isEmpty()) {
                        bukkitPlayer.sendBlockChange(new Location(bukkitPlayer.getWorld(), 0, 0, 0), bukkitPlayer.getWorld().getBlockData(0, 0, 0));
                        player.getParty().handleError(Party.Error.EMPTY_POLL_QUESTION);
                        return;
                    }

                    Bukkit.getServer().getScheduler().runTask(GAPIPlugin.this, new Runnable() {
                        @Override
                        public void run() {
                            bukkitPlayer.sendBlockChange(new Location(bukkitPlayer.getWorld(), 0, 0, 0), bukkitPlayer.getWorld().getBlockData(0, 0, 0));
                            player.getParty().data.getAnswers().add(new PollAnswer(player.getParty().data, builder.toString()));
                            player.getParty().openGUI(player.getParty().data);
                            player.getParty().data = null;
                        }
                    });
                }
            }
        });

        GameManager.loadGames();
    }

    private boolean deleteWorld(File path) {
        if (path.exists()) {
            File files[] = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteWorld(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    @Override
    public void onDisable() {
        Bukkit.getServer().getScheduler().cancelTasks(this);
        for (GameBase base : GameManager.GAMES) {
            if (base.hasLobbySupport()) {
                for (Lobby lobby : base.getLobbyInstances()) {
                    lobby.delete();
                }
            }
            for (ArrayList<GameInstance> instances : base.getRunningInstances().values()) {
                for (GameInstance instance : instances) {
                    Bukkit.getServer().getScheduler().cancelTask(instance.eventsTaskID);
                    if (instance.getMap() != null) {
                        if (instance.getMap().getBukkitWorld().getPlayers().size() != 0) {
                            for (Player player : instance.getMap().getBukkitWorld().getPlayers()) {
                                player.kickPlayer("§cGameAPI shutting down");
                                //Move to fallback server...
                            }
                            Bukkit.unloadWorld(instance.getMap().getBukkitWorld(), false);
                            deleteWorld(instance.getMap().getBukkitWorld().getWorldFolder());
                        }
                    }
                }
            }
        }
    }
}

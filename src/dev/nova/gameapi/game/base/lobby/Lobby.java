package dev.nova.gameapi.game.base.lobby;

import dev.nova.gameapi.game.base.lobby.core.LobbyBase;
import dev.nova.gameapi.game.player.GamePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class Lobby {

    private final LobbyBase lobbyBase;
    private final World world;
    private final int id;

    public Lobby(LobbyBase lobbyBase, int id){
        this.lobbyBase = lobbyBase;
        this.id = id;
        this.world = createBukkitWorld();
    }

    public World getWorld() {
        return world;
    }

    public int getId() {
        return id;
    }

    private World createBukkitWorld() {
        File copiedFile = new File(Bukkit.getWorldContainer(), "lobby-"+lobbyBase.game().getCodeName()+"-"+id);
        World oldWorld = Bukkit.getServer().getWorld(lobbyBase.bukkitWorld().getName());

        copiedFile.mkdir();
        copyFileStructure(oldWorld.getWorldFolder(), copiedFile);

        WorldCreator creator = new WorldCreator("lobby-"+lobbyBase.game().getCodeName()+"-"+id);
        World newWorld = creator.createWorld();

        newWorld.setAutoSave(false);
        newWorld.setGameRuleValue("doDaylightCycle", "false");
        newWorld.setGameRuleValue("doWeatherCycle", "false");
        newWorld.setDifficulty(Difficulty.EASY);
        newWorld.setTime(1000);
        newWorld.setGameRuleValue("announceAdvancements","false");
        newWorld.setThundering(false);
        newWorld.setStorm(false);
        newWorld.setWeatherDuration(-1);

        return newWorld;
    }

    private static void copyFileStructure(File source, File target) {
        try {
            ArrayList<String> ignore = new ArrayList<>(Arrays.asList("uid.dat", "session.lock"));
            if (!ignore.contains(source.getName())) {
                if (source.isDirectory()) {
                    if (!target.exists())
                        if (!target.mkdirs())
                            throw new IOException("Couldn't create world directory!");
                    String files[] = source.list();
                    for (String file : files) {
                        File srcFile = new File(source, file);
                        File destFile = new File(target, file);
                        copyFileStructure(srcFile, destFile);
                    }
                } else {
                    InputStream in = new FileInputStream(source);
                    OutputStream out = new FileOutputStream(target);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0)
                        out.write(buffer, 0, length);
                    in.close();
                    out.close();
                }
            }
        } catch (IOException e) {
        }
    }

    private boolean deleteWorld(File path) {
        if(path.exists()) {
            File files[] = path.listFiles();
            for(int i=0; i<files.length; i++) {
                if(files[i].isDirectory()) {
                    deleteWorld(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return(path.delete());
    }

    public void delete() {
        for(Player player : world.getPlayers()){
            player.kick(Component.text("§cLobby shutting down!"));
        }

        Bukkit.getServer().unloadWorld(world,false);
        deleteWorld(world.getWorldFolder());
    }

    public void move(GamePlayer player) {
        player.getPlayer().sendMessage("§aYou were moved to lobby: "+id);
        player.getPlayer().setHealth(20L);
        player.getPlayer().setFoodLevel(20);
        player.getPlayer().setSaturation(120F);
        player.getPlayer().getInventory().clear();
        player.getPlayer().setGameMode(GameMode.ADVENTURE);
        player.getPlayer().teleport(lobbyBase.parseLocation(this));
    }
}

package dev.nova.gameapi.game.map.command;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.GameMap;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MapEditCommand implements CommandExecutor {

    private static final HashMap<Player,GameMap> editors = new HashMap<>();
    private static final HashMap<Player,World> editorsWorlds = new HashMap<>();
    private static final HashMap<Player, Location> editorsLocations = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if(!(commandSender instanceof Player)){
            commandSender.sendMessage("§cOnly players can execute commands!");
            return true;
        }

        Player player = (Player) commandSender;

        if(args.length == 0){
            player.sendMessage("§cPlease specify an option!");
            return true;
        }

        if(args.length == 1){
            if(args[0].equalsIgnoreCase("edit")){
                player.sendMessage("§cMissing game & map name!");
                return true;
            }

            boolean save;

            if(args[0].equalsIgnoreCase("save")){
                save = true;
            }else if(args[0].equalsIgnoreCase("discard")){
                save = false;
            }else{
                player.sendMessage("§cUnknown option!");
                return true;
            }

            if(!editors.containsKey(player)){
                player.sendMessage("§cYou are not editing any maps!");
                return true;
            }

            GameMap map = editors.get(player);



            if(save) {
                for(Player player1 : map.getWorld().getPlayers()){
                    player1.teleport(editorsLocations.get(player));
                    player1.sendMessage("§aThe map is being saved!");
                }
                for(Player player1 : editorsWorlds.get(player).getPlayers()){
                    player1.teleport(editorsLocations.get(player));
                    player1.sendMessage("§aThe map is being saved!");
                }
                player.sendMessage("§aMap saved successfully!");
            }else{
                player.sendMessage("§cMap modification discarded!");
                for(Player player1 : map.getWorld().getPlayers()){
                    player1.teleport(editorsLocations.get(player));
                    player1.sendMessage("§aThe map is being reverted to its previous state!");
                }
                for(Player player1 : editorsWorlds.get(player).getPlayers()){
                    player1.teleport(editorsLocations.get(player));
                    player1.sendMessage("§aThe map is being reverted to its previous state!");
                }
            }

            Bukkit.unloadWorld(editorsWorlds.get(player),true);
            Bukkit.unloadWorld(map.getWorld(),false);

            deleteWorld(map.getWorld().getWorldFolder());

            try {
                cloneWorld(editorsWorlds.get(player),map.getFileName());
            } catch (GameMap.Map.UnableToCloneException e) {
                e.printStackTrace();
            }

            editors.remove(player);
            editorsWorlds.remove(player);
            editorsLocations.remove(player);
        }

        if(args.length == 3){
            if(args[0].equalsIgnoreCase("edit")){
                GameBase base = GameManager.getGame(args[1]);

                if(base == null){
                    player.sendMessage("§cNo game exists with this name!");
                    return true;
                }

                GameMap map = base.getMap(args[2]);

                if(map == null){
                    player.sendMessage("§cNo map exists with this name!");
                    return true;
                }

                if(!editors.containsKey(player)){
                    editors.put(player,map);
                    editorsLocations.put(player, player.getLocation());


                    player.sendMessage("§aSending you to map base world: "+map.getWorld().getName());

                    World clone;

                    try {
                        clone = cloneWorld(map.getWorld(),map.getWorld().getName()+"-edit-"+editors.size()+1);
                    } catch (GameMap.Map.UnableToCloneException e) {
                        player.sendMessage("§cUnable to clone!");
                        return true;
                    }

                    editorsWorlds.put(player,clone);

                    clone.setSpawnLocation(map.getWorld().getSpawnLocation());

                    player.teleport(clone.getSpawnLocation());
                    player.setAllowFlight(true);
                    player.setGameMode(GameMode.CREATIVE);
                    player.setFlying(true);
                    return true;
                }else{
                    player.sendMessage("You are already editing a map!, do /mapEdit discard or save to be able to edit again!");
                    return true;
                }

            }
        }

        return true;
    }

    private static boolean deleteWorld(File path) {
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

    private static World cloneWorld(World world,String name) throws GameMap.Map.UnableToCloneException {
        File copiedFile = new File(Bukkit.getWorldContainer(), name);

        World old = Bukkit.getWorld(name);

        if(old != null){
            Bukkit.unloadWorld(old,false);
            deleteWorld(old.getWorldFolder());
        }

        copiedFile.mkdir();
        copyFileStructure(world.getWorldFolder(), copiedFile);

        World newWorld = new WorldCreator(world.getName()+"-edit-"+editors.size()+1).createWorld();

        newWorld.setAutoSave(false);

        return newWorld;
    }

    private static void copyFileStructure(File source, File target) throws GameMap.Map.UnableToCloneException {
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
            throw new GameMap.Map.UnableToCloneException(e);
        }
    }
}

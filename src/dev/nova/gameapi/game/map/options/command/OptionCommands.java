package dev.nova.gameapi.game.map.options.command;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.map.options.OptionType;
import dev.nova.gameapi.utils.Files;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.swing.text.html.Option;
import java.io.File;
import java.io.IOException;

public class OptionCommands implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(command.getName().equalsIgnoreCase("setOption")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can set options!");
                return true;
            }

            Player player = (Player) sender;

            player.sendMessage("§bYou are in: §6"+player.getWorld().getName());

            if (args.length == 0) {
                player.sendMessage("§cMissing game name!");
                return true;
            }
//0
            if (args.length == 1) {
                player.sendMessage("§cMissing map name!");
                return true;
            }
//0,1
            if (args.length == 2) {
                player.sendMessage("§cMissing option key!");
                return true;
            }
//0,1,2
            if (args.length == 3) {
                player.sendMessage("§cMissing option type!");
                return true;
            }

            GameBase base = GameManager.getGame(args[0]);
            if(base == null){
                player.sendMessage("§cNo game exists with name: "+args[0]);
                return true;
            }

            GameMap map = base.getMap(args[1]);
            if(map == null){
                player.sendMessage("§cNo map exists with name: "+args[1]+" on game: "+args[0]);
                return true;
            }
            OptionType type;

            try {
                type = OptionType.valueOf(args[3].toUpperCase());
            }catch (IllegalArgumentException e){
                player.sendMessage("No option type exists with name: "+args[3].toUpperCase());
                return true;
            }

            File mapFile = new File(Files.getGameMapsFolder(base.getCodeName()),map.getFile().getName());

            YamlConfiguration configuration = new YamlConfiguration();

            try{
                configuration.load(mapFile);
            }catch (IOException | InvalidConfigurationException e){
                player.sendMessage("§cUnable to load map configuration!");
                e.printStackTrace();
                return true;
            }

            if(args.length == 4){
                if(type == OptionType.LOCATION){

                    if(!player.getWorld().equals(Bukkit.getWorld(configuration.getConfigurationSection("data").getString("world")))){
                        player.sendMessage("§cYour world must be the same as the map's to set a location option!");
                        return true;
                    }

                    configuration.set("data.options."+args[2]+".type",type.name());
                    configuration.set("data.options."+args[2]+".value",player.getLocation());
                    player.sendMessage("§aSuccessfully set option: "+args[2]+" to your location!");
                    try {
                        configuration.save(mapFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }else if(args.length > 4){

            }
        }

        return true;
    }
}

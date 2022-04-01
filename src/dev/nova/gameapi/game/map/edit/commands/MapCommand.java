package dev.nova.gameapi.game.map.edit.commands;

import com.grinderwolf.swm.api.exceptions.WorldAlreadyExistsException;
import com.grinderwolf.swm.api.world.SlimeWorld;
import dev.nova.gameapi.GameAPI;
import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.lobby.LobbyBase;
import dev.nova.gameapi.game.manager.GameManager;

import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.map.edit.map.EditingMap;
import dev.nova.gameapi.game.map.options.OptionType;
import dev.nova.gameapi.game.map.options.instance.GameOptionInfo;
import dev.nova.gameapi.game.map.swm.GameLocation;
import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.utils.Files;
import dev.nova.gameapi.utils.api.general.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapCommand implements CommandExecutor, TabCompleter {

    public String combineArgs(int start, String[] args) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            if (i < start) {
                continue;
            }
            builder.append(args[i]).append(i != args.length - 1 ? " " : "");
        }

        return builder.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player bukkitPlayer)) {
            sender.sendMessage(ChatColor.RED + "Only players can edit maps!");
            return true;
        }

        GamePlayer player = GamePlayer.getPlayer(bukkitPlayer);

        if (args.length == 0) {
            bukkitPlayer.sendMessage(ChatColor.RED + "Please specify an action.");
            return true;
        }

        if(args[0].equalsIgnoreCase("lobby")) {


            if(args.length == 1) {
                bukkitPlayer.sendMessage(ChatColor.RED+"Please specify a game!");
                return true;
            }

            GameBase base = GameManager.getGame(args[1]);

            if(base == null) {
                bukkitPlayer.sendMessage(ChatColor.RED+"No game exists with this name!");
                return true;
            }

            if(!base.hasLobbySupport()) {
                bukkitPlayer.sendMessage(ChatColor.RED+"This game does not have lobby support.");
                return true;
            }

            if(args.length == 2){
                bukkitPlayer.sendMessage(ChatColor.RED+"Please specify a lobby name!");
                return true;
            }

            LobbyBase lobbyBase = base.getLobbyManager().getLobbyBaseByName(args[2]);

            if(args.length == 3){
                bukkitPlayer.sendMessage(ChatColor.RED+"Please specify an action!");
                return true;
            }

            if(args.length == 4 && !(args[3].equalsIgnoreCase("create"))) {
                if (lobbyBase == null) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "No lobby exists with this name!");
                    return true;
                }
            }

            if(args[3].equalsIgnoreCase("edit")){
                GameAPI.MAP_EDITING_MANAGER.editMap(player, lobbyBase);
            }else if(args[3].equalsIgnoreCase("stop")) {
                if (!GameAPI.MAP_EDITING_MANAGER.isMapBeingEdited(lobbyBase)) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "This map is not being edited!");
                    return true;
                }

                GameAPI.MAP_EDITING_MANAGER.forceStop(bukkitPlayer, lobbyBase);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("forceExit")) {
            if (!GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().containsKey(player)) {
                bukkitPlayer.sendMessage(ChatColor.RED + "You are not editing any map!");
                return true;
            }
            bukkitPlayer.sendMessage(ChatColor.GREEN + "Force exiting you from map editing...");
            GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().get(player).close(false);
        } else if (args[0].equalsIgnoreCase("forceSave")) {
            if (!GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().containsKey(player)) {
                bukkitPlayer.sendMessage(ChatColor.RED + "You are not editing any map!");
                return true;
            }

            bukkitPlayer.sendMessage(ChatColor.GREEN + "Force saving the map in the current state...");
            GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().get(player).close(true);
        } else if (args[0].equalsIgnoreCase("exit")) {
            if (!GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().containsKey(player)) {
                bukkitPlayer.sendMessage(ChatColor.RED + "You are not editing any map!");
                return true;
            }
            GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().get(player).attemptExit();
        } else if (args[0].equalsIgnoreCase("message")) {
            if (!GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().containsKey(player)) {
                bukkitPlayer.sendMessage(ChatColor.RED + "You are not editing any map!");
                return true;
            }
            GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().get(player).sendOptionsMessage(args.length == 2 && args[1].equalsIgnoreCase("all"));
        } else if (args[0].equalsIgnoreCase("stop")) {

            if (args.length == 1) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify a game.");
                return true;
            }

            GameBase base = GameManager.getGame(args[1]);

            if (args.length == 2) {

                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify an instance.");
                return true;
            }



            if (args.length == 3) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify a map.");
                return true;
            }

            if (args.length == 4) {

                if (base == null) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "Unknown game!");
                    return true;
                }

                if (!base.getEditingGameMaps().containsKey(args[2])) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "Unknown instance!");
                    return true;
                }


                if (!base.containsEditingMap(args[2], args[3]) && base.getMap(args[2], args[3]) == null) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "No map exists with this name!");
                    return true;
                }

                GameMap map = base.getMap(args[2], args[3]) == null ? base.getEditingMap(args[2], args[3]) : base.getMap(args[2], args[3]);

                if (!GameAPI.MAP_EDITING_MANAGER.isMapBeingEdited(map)) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "This map is not being edited!");
                    return true;
                }

                GameAPI.MAP_EDITING_MANAGER.forceStop(bukkitPlayer, map);
            }
        } else if (args[0].equalsIgnoreCase("edit")) {

            if (args.length == 1) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify a game.");
                return true;
            }

            if (args.length == 2) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify an instance.");
                return true;
            }

            if (args.length == 3) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify a map.");
                return true;
            }

            if (args.length == 4) {
                GameBase base = GameManager.getGame(args[1]);

                if (base == null) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "Unknown game!");
                    return true;
                }

                if (!base.getEditingGameMaps().containsKey(args[2])) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "Unknown instance!");
                    return true;
                }

                if (!base.containsEditingMap(args[2], args[3]) && base.getMap(args[2], args[3]) == null) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "No map exists with this name!");
                    return true;
                }

                GameAPI.MAP_EDITING_MANAGER.editMap(player, base.getMap(args[2], args[3]) == null ? base.getEditingMap(args[2], args[3]) : base.getMap(args[2], args[3]));

                return true;
            }

            bukkitPlayer.sendMessage(ChatColor.RED + "Way too many arguments!");
        } else if (args[0].equalsIgnoreCase("set")) {
            if (!GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().containsKey(player)) {
                bukkitPlayer.sendMessage(ChatColor.RED + "You are not editing any map!");
                return true;
            }

            EditingMap map = GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().get(player);

            if (args.length == 1) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify an option key!");
                return true;
            }

            GameOptionInfo option = map.getOption(args[1]);

            if (option == null) {
                bukkitPlayer.sendMessage(ChatColor.RED + "No option exists with that key.");
                return true;
            }

            if (option.getOptionSpecificInfo() instanceof GameOptionInfo.OptionSpecificInfo.Array array) {
                if (args.length == 2) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "Select a value to remove. 0-" + (array.getLength() == 0 ? "Unlimited" : map.getLength(array, option)));
                    return true;
                }

                try {
                    Integer value = Integer.valueOf(args[2]);
                    ConfigurationSection values = map.getTemporaryConfig().getConfigurationSection(option.getKey());

                    if (!values.contains(String.valueOf(value))) {
                        bukkitPlayer.sendMessage(ChatColor.RED + "Unknown value.");
                        return true;
                    }

                    if (args.length >= 4) {
                        if (setOption(option, bukkitPlayer, (option.getKey() + "." + value), Arrays.copyOfRange(args, 3, args.length), map)) {
                            bukkitPlayer.sendMessage(ChatColor.GREEN + "Successfully set the value of: " + option.getRealDisplayName() + " at: " + value);
                        }
                        return true;
                    } else {
                        bukkitPlayer.sendMessage(ChatColor.RED + "There is no value to set.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "Unknown value.");
                }
            }

            if (args.length >= 3) {
                if (setOption(option, bukkitPlayer, option.getKey(), Arrays.copyOfRange(args, 2, args.length), map)) {
                    bukkitPlayer.sendMessage(ChatColor.GREEN + "Successfully set the value of: " + option.getRealDisplayName());
                }
            } else {
                bukkitPlayer.sendMessage(ChatColor.RED + "There is no value to set.");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("create")) {
            if (GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().containsKey(player)) {
                bukkitPlayer.sendMessage(ChatColor.RED + "You are already editing another map!");
                return true;
            }

            if (args.length == 1) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify a game.");
                return true;
            }

            if (args.length == 2) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify an instance.");
                return true;
            }

            if (args.length == 3) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify a code name.");
                return true;
            }

            if (args.length == 4) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify a player limit.");
                return true;
            }

            if (args.length == 5) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify a display name.");
                return true;
            }

            int limit;

            try {
                limit = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                bukkitPlayer.sendMessage(ChatColor.RED + args[4] + " is not a number");
                return true;
            }
            GameBase base = GameManager.getGame(args[1]);

            if (base == null) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Unknown game!");
                return true;
            }

            if (!base.getEditingGameMaps().containsKey(args[2])) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Unknown instance!");
                return true;
            }

            if (base.containsEditingMap(args[2], args[3]) || base.getMap(args[2], args[3]) != null) {
                bukkitPlayer.sendMessage(ChatColor.RED + "A map already exists with that name.");
                return true;
            }

            File file = new File(Files.getGameMapsFolder(base.getCodeName(), args[2]), args[3] + ".yml");

            try {
                file.createNewFile();

                YamlConfiguration configuration = new YamlConfiguration();
                configuration.set("data.code-name", args[3]);
                configuration.set("data.world", args[3]);
                configuration.set("data.player-limit", limit);
                configuration.set("data.display-name", combineArgs(5, args));
                configuration.save(file);

                Bukkit.getServer().getScheduler().runTaskAsynchronously(GameAPI.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SlimeWorld newWorld = GameAPI.getSlime().createEmptyWorld(GameManager.SLIME_FILE_LOADER, args[3], false, GameManager.GENERIC_PROPERTY_MAP);

                            GameMap map = new GameMap(base, file, configuration.getConfigurationSection("data"), args[2]);

                            GameManager.SLIME_WORLD_MAPS.put(map, newWorld);
                            base.getEditingGameMaps().get(args[2]).add(map);
                            GameManager.reloadMap(null, map);

                            bukkitPlayer.sendMessage(ChatColor.GREEN + "Created new map for game: " + args[2] + " named: " + map.getDisplayName());

                        } catch (WorldAlreadyExistsException | IOException e) {
                            bukkitPlayer.sendMessage(ChatColor.RED + "Unable to create world.");
                            e.printStackTrace();
                        }
                    }
                });
                return true;
            } catch (IOException e) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Unable to create config file");
                e.printStackTrace();
            }

            return true;

        } else if (args[0].equalsIgnoreCase("remove")) {
            if (!GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().containsKey(player)) {
                bukkitPlayer.sendMessage(ChatColor.RED + "You are not editing any map!");
                return true;
            }

            EditingMap map = GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().get(player);

            if (args.length == 1) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Please specify an option key!");
                return true;
            }

            GameOptionInfo option = map.getOption(args[1]);

            if (option == null) {
                bukkitPlayer.sendMessage(ChatColor.RED + "No option exists with that key.");
                return true;
            }

            if (option.getOptionSpecificInfo() instanceof GameOptionInfo.OptionSpecificInfo.Array) {
                if (args.length == 2) {
                    ConfigurationSection values = map.getTemporaryConfig().getConfigurationSection(option.getKey());
                    bukkitPlayer.sendMessage(ChatColor.RED + "Select a value to remove. 0-" + (values.getKeys(false).size() - 1));
                    return true;
                }

                try {
                    Integer value = Integer.valueOf(args[2]);
                    ConfigurationSection values = map.getTemporaryConfig().getConfigurationSection(option.getKey());

                    if (!values.contains(String.valueOf(value))) {
                        bukkitPlayer.sendMessage(ChatColor.RED + "Unknown value.");
                        return true;
                    }

                    bukkitPlayer.sendMessage(ChatColor.GREEN + "Successfully deleted value: " + value + " from: " + option.getRealDisplayName());
                    values.set(String.valueOf(value), null);
                } catch (NumberFormatException e) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "Unknown value.");
                }
                return true;
            }

            if (!map.getTemporaryConfig().contains(option.getKey())) {
                bukkitPlayer.sendMessage(ChatColor.RED + "This option is not set!");
                return true;
            }

            bukkitPlayer.sendMessage(ChatColor.GREEN + "Successfully deleted: " + option.getRealDisplayName());
            map.getTemporaryConfig().set(option.getKey(), null);
        } else {
            bukkitPlayer.sendMessage(ChatColor.RED + "Unknown action!");
        }


        return true;
    }

    private boolean setOption(GameOptionInfo option, Player bukkitPlayer, String key, String[] args, EditingMap map) {
        GameOptionInfo parent = getFirstParent(option);
        map.getTemporaryConfig().set(parent.getOriginalKey()+".type",parent.getOptionType().name());
        OptionType type = determineType(option);
        if (type.equals(OptionType.ARRAY)) {
            bukkitPlayer.sendMessage(ChatColor.RED+"Multidimensional arrays cannot be set, yet.");
            return false;
        }else if(type.equals(OptionType.ITEM)) {
            if(args[0].equalsIgnoreCase("hand")) {
                ItemStack item = bukkitPlayer.getItemInHand();

                if(item == null){
                    bukkitPlayer.sendMessage(ChatColor.RED+"Item is null.");
                    return false;
                }

                map.getTemporaryConfig().set(key,item);
                return true;
            }else{
                ItemStack stack = ItemBuilder.getFromJSON(bukkitPlayer,combineArgs(args));

                if(stack == null){
                    bukkitPlayer.sendMessage(ChatColor.RED+"Unable to parse item from JSON!");
                    return false;
                }

                map.getTemporaryConfig().set(key,stack);
                return true;
            }
        }else if(type.equals(OptionType.VECTOR)) {

            if (args.length < 3) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Missing vector information, need x y z ");
                return false;
            }

            double x, y, z;
            try {
                x = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                try {
                    x = Integer.parseInt(args[0]);
                } catch (NumberFormatException ex) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "Invalid X value.");
                    return false;
                }
            }

            try {
                y = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                try {
                    y = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "Invalid X value.");
                    return false;
                }
            }

            try {
                z = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                try {
                    z = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "Invalid X value.");
                    return false;
                }
            }

            Vector vector = new Vector(x,y,z);
            map.getTemporaryConfig().set(key, vector);
            return true;
        }else if(type.equals(OptionType.DOUBLE)) {

            double value;

            try {
                value = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                bukkitPlayer.sendMessage(ChatColor.RED+"Invalid double value.");
                return false;
            }

            map.getTemporaryConfig().set(key,value);
            return true;
        }else if(type.equals(OptionType.FLOAT)) {
            float value;

            try {
                value = Float.parseFloat(args[0]);
            } catch (NumberFormatException e) {
                bukkitPlayer.sendMessage(ChatColor.RED+"Invalid float value.");
                return false;
            }

            map.getTemporaryConfig().set(key,value);
            return true;
        } else if (type.equals(OptionType.CONFIGURATION_SECTION)) {
            bukkitPlayer.sendMessage(ChatColor.RED + "Configuration Sections cannot be set directly, you may only set the values inside of them.");
            return false;
        } else if (type.equals(OptionType.LOCATION)) {
            if (args.length < 3) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Missing location information, need x y z [yaw pitch]");
                return false;
            }

            double x, y, z;
            float yaw = 0, pitch = 0;

            if (args[0].equalsIgnoreCase("~")) {
                x = bukkitPlayer.getLocation().getX();
            } else if (args[0].equalsIgnoreCase("^")) {
                x = bukkitPlayer.getLocation().getBlockX();
            } else if (args[0].equalsIgnoreCase("@")) {
                x = bukkitPlayer.getLocation().getBlockX() + 0.5;
            } else {
                try {
                    x = Double.parseDouble(args[0]);
                } catch (NumberFormatException e) {
                    try {
                        x = Integer.parseInt(args[0]);
                    } catch (NumberFormatException ex) {
                        bukkitPlayer.sendMessage(ChatColor.RED + "Invalid X value.");
                        return false;
                    }
                }
            }


            if (args[1].equalsIgnoreCase("~")) {
                y = bukkitPlayer.getLocation().getX();
            } else if (args[1].equalsIgnoreCase("^")) {
                y = bukkitPlayer.getLocation().getBlockX();
            } else if (args[1].equalsIgnoreCase("@")) {
                y = bukkitPlayer.getLocation().getBlockX() + 0.5;
            } else {
                try {
                    y = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    try {
                        y = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ex) {
                        bukkitPlayer.sendMessage(ChatColor.RED + "Invalid Y value.");
                        return false;
                    }
                }
            }


            if (args[2].equalsIgnoreCase("~")) {
                z = bukkitPlayer.getLocation().getX();
            } else if (args[2].equalsIgnoreCase("^")) {
                z = bukkitPlayer.getLocation().getBlockX();
            } else if (args[2].equalsIgnoreCase("@")) {
                z = bukkitPlayer.getLocation().getBlockX() + 0.5;
            } else {
                try {
                    z = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    try {
                        z = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ex) {
                        bukkitPlayer.sendMessage(ChatColor.RED + "Invalid Z value.");
                        return false;
                    }
                }
            }

            if (args.length == 5) {
                if (args[3].equalsIgnoreCase("north")) {
                    yaw = 180;
                } else if (args[3].equalsIgnoreCase("east")) {
                    yaw = -90;
                } else if (args[3].equalsIgnoreCase("south")) {
                    yaw = 0;
                } else if (args[3].equalsIgnoreCase("west")) {
                    yaw = 90;
                } else {
                    try {
                        pitch = Double.valueOf(args[3]).floatValue();
                    } catch (NumberFormatException e) {
                        try {
                            pitch = Integer.parseInt(args[3]);
                        } catch (NumberFormatException ex) {
                            bukkitPlayer.sendMessage(ChatColor.RED + "Invalid Yaw value.");
                            return false;
                        }
                    }
                }

                if (args[4].equalsIgnoreCase("up")) {
                    pitch = -90;
                } else if (args[4].equalsIgnoreCase("down")) {
                    pitch = 90;
                } else {
                    try {
                        pitch = Double.valueOf(args[4]).floatValue();
                    } catch (NumberFormatException e) {
                        try {
                            pitch = Integer.parseInt(args[4]);
                        } catch (NumberFormatException ex) {
                            bukkitPlayer.sendMessage(ChatColor.RED + "Invalid Pitch value.");
                            return false;
                        }
                    }
                }
            }

            GameLocation location = new GameLocation(x, y, z, yaw, pitch);

            map.getTemporaryConfig().set(key, location);
            return true;
        } else if (type.equals(OptionType.STRING)) {
            map.getTemporaryConfig().set(key, combineArgs(args));
        } else if (type.equals(OptionType.BOOLEAN)) {
            boolean set;

            if (args[0].equalsIgnoreCase("yes")) {
                set = true;
            } else if (args[0].equalsIgnoreCase("no")) {
                set = false;
            } else if (args[0].equalsIgnoreCase("true")) {
                set = true;
            } else if (args[0].equalsIgnoreCase("false")) {
                set = false;
            } else {
                bukkitPlayer.sendMessage(ChatColor.RED + "A boolean can be either true or false.");
                return false;
            }

            map.getTemporaryConfig().set(key, set);
            return true;
        } else if (type.equals(OptionType.INTEGER)) {
            try {
                map.getTemporaryConfig().set(key, Integer.parseInt(args[0]));
                return true;
            } catch (NumberFormatException e) {
                bukkitPlayer.sendMessage(ChatColor.RED + args[0] + " is not an integer.");
                return false;
            }
        } else {
            bukkitPlayer.sendMessage(ChatColor.RED + "Do not know how set these option types.");
            return false;
        }

        return true;
    }

    private GameOptionInfo getFirstParent(GameOptionInfo option) {
        if(option.getParent() == null) {
            return option;
        }

        GameOptionInfo info = option;

        while (info.getParent() != null) {
            info = info.getParent();
        }

        return info;
    }

    private String combineArgs(String[] args) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            builder.append(args[i]).append(i != args.length - 1 ? " " : "");
        }

        return builder.toString();
    }

    private OptionType determineType(GameOptionInfo option) {
        if (option.getOptionSpecificInfo() instanceof GameOptionInfo.OptionSpecificInfo.Array array) {
            return array.getType();
        }
        return option.getOptionType();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> list = new ArrayList<>();

        if(!(sender instanceof Player bukkitPlayer)) {
            return list;
        }

        GamePlayer player = GamePlayer.getPlayer(bukkitPlayer);

        if(args.length == 1) {
            if(!GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().containsKey(player)){
                list.add("edit");
                list.add("create");
                list.add("lobby");
            }else{
                list.add("set");
                list.add("remove");
                list.add("exit");
                list.add("forceExit");
            }

            list.add("forceStop");
        }

        if(args.length == 2) {
            if(args[0].equalsIgnoreCase("lobby") || args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("create")) {
                for (GameBase base : GameManager.GAMES) {
                    list.add(base.getCodeName());
                }
            }else if(args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) {
                if(GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().containsKey(player)){
                    EditingMap map = GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().get(player);

                    for(GameOptionInfo info : map.getOptions()) {
                        list.add(info.getKey());
                    }
                }
            }
        }

        if(args.length == 3) {
            if(args[0].equalsIgnoreCase("lobby")) {
                GameBase base = GameManager.getGame(args[1]);

                if(base == null) {
                    return list;
                }

                for(LobbyBase base1 : base.getLobbyManager().editingLobbies) {
                    list.add(base1.getCodeName());
                }

                for(LobbyBase base1 : base.getLobbyManager().getLobbyBases()) {
                    list.add(base1.getCodeName());
                }
            }else if(args[0].equalsIgnoreCase("set")){

            }else if(args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("edit")) {
                GameBase base = GameManager.getGame(args[1]);

                if(base == null) {
                    return list;
                }

                list.addAll(base.getInstances().keySet());
            }
        }

        if(args.length == 4) {
            if(args[0].equalsIgnoreCase("lobby")) {
                list.add("stop");
                list.add("edit");
            }else if(args[0].equalsIgnoreCase("edit")) {
                GameBase base = GameManager.getGame(args[1]);

                if(base == null) {
                    return list;
                }



                for(GameMap map : base.getEditingGameMaps().getOrDefault(args[2],base.getGameMaps().getOrDefault(args[2],new ArrayList<>()))) {
                    list.add(map.getCodeName());
                }
            }
        }

        return list;
    }
}

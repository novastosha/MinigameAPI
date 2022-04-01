package dev.nova.gameapi.game.base.lobby;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.lobby.mode.LobbyInfo;
import dev.nova.gameapi.game.base.lobby.options.LobbyOption;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.Map;
import dev.nova.gameapi.game.map.options.OptionType;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class LobbyBase implements Map {

    private final File file;

    @Override
    public GameBase getGameBase() {
        return manager.getGame();
    }

    @Override
    public String getCodeName() {
        return info.name();
    }

    @Override
    public String getDisplayName() {
        return "Lobby " + info.name();
    }

    public ConfigurationSection getOptionsSection() {
        return data.getConfigurationSection("options") == null ? data.createSection("options") : data.getConfigurationSection("options");
    }

    public void updateOptions(ConfigurationSection temporaryConfig) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            data.set("options", temporaryConfig);
            configuration.set("data", data);
            configuration.save(file);
            updateRawData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void updateRawData() {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            return;
        }
        data = configuration.getConfigurationSection("data");

    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public ConfigurationSection getRawData() {
        return data;
    }


    public static class Command implements CommandExecutor, TabCompleter {

        @Override
        public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
            if (!(sender instanceof Player bukkitPlayer)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            GamePlayer player = GamePlayer.getPlayer(bukkitPlayer);

            if (args.length == 0) {
                if (player.getGame() != null) {
                    player.getGame().getGameBase().moveToLobby(player);
                } else {
                    player.getPlayer().sendMessage(ChatColor.RED + "Do not know where to move you!");
                    return true;
                }
            }

            GameBase base = GameManager.getGame(args[0]);

            if (base == null) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Unknown game!");
                return true;
            }

            try {
                if (!base.moveToLobby(player)) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "Couldn't move you to the lobby!");
                }
                return true;
            } catch (Exception e) {
                bukkitPlayer.sendMessage(ChatColor.RED + "Couldn't move you to the lobby!");
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {

            List<String> list = new ArrayList<>();

            for (GameBase base : GameManager.GAMES) {
                if (base.hasLobbySupport()) {
                    list.add(base.getCodeName());
                }
            }

            return list;
        }
    }

    private final GameBase.LobbyManager manager;
    private final LobbyInfo info;
    private ConfigurationSection data;
    private final ArrayList<LobbyOption> options;

    public LobbyBase(GameBase.LobbyManager manager, LobbyInfo lobbyInfo, ConfigurationSection data, File file) {
        this.manager = manager;
        this.file = file;
        this.info = lobbyInfo;
        this.data = data;
        this.options = new ArrayList<LobbyOption>();

    }

    public ArrayList<LobbyOption> getOptions() {
        return options;
    }

    public LobbyOption getOption(String codeName) {
        for (LobbyOption option : options) {
            if (option.getKey().equalsIgnoreCase(codeName)) return option;
        }
        return null;
    }

    public LobbyOption loadOption(String key, OptionType type, boolean required) {
        LobbyOption option = null;
        if (data.getConfigurationSection("options") != null) {
            ConfigurationSection options = data.getConfigurationSection("options");

            ConfigurationSection optionConfig = options.getConfigurationSection(key);

            if (optionConfig == null) {
                return null;
            }
            OptionType optionType;

            try {
                optionType = OptionType.valueOf(optionConfig.getString("type").toUpperCase());

            } catch (IllegalArgumentException e) {
                return null;
            }

            if (!optionType.equals(type)) {
                return null;
            }

            if (optionType.equals(OptionType.ARRAY) && optionConfig.getConfigurationSection("value") == null) {
                return null;
            }

            option = new LobbyOption(key, optionType, optionConfig.get("value"), this);

            this.options.add(option);
        }

        if (option == null && required) {
            manager.shutDownBase(this, "option: " + key + ", of type: " + type.name() + " is missing!");
        }

        return option;
    }

    public Location getSpawn(Lobby lobby) {
        return ((LobbyLocation) data.get("spawn")).toBukkitLocation(lobby);
    }

    public LobbyInfo getInfo() {
        return info;
    }

    public GameBase.LobbyManager getManager() {
        return manager;
    }
}

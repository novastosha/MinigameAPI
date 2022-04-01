package dev.nova.gameapi.game.map.edit.map;

import com.grinderwolf.swm.api.world.SlimeWorld;
import dev.nova.gameapi.GameAPI;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.lobby.LobbyBase;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.map.Map;
import dev.nova.gameapi.game.map.options.OptionType;
import dev.nova.gameapi.game.map.options.instance.GameOptionInfo;
import dev.nova.gameapi.game.map.swm.GameLocation;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EditingMap implements Listener {
    private final GamePlayer editor;
    private final World editWorld;
    private final Map map;
    private final ConfigurationSection temporaryConfig;
    private final SlimeWorld slimeWorld;

    public EditingMap(GamePlayer editor, World editWorld, Map map, SlimeWorld slimeWorld) {
        this.editor = editor;
        this.slimeWorld = slimeWorld;
        this.temporaryConfig = map.getOptionsSection();
        this.editWorld = editWorld;
        this.map = map;

        editWorld.setAutoSave(false);
        editWorld.setGameRuleValue("doDaylightCycle", "false");
        editWorld.setGameRuleValue("doWeatherCycle", "false");
        editWorld.setDifficulty(Difficulty.PEACEFUL);
        editWorld.setTime(1000);
        editWorld.setGameRuleValue("announceAdvancements", "false");
        editWorld.setThundering(false);
        editWorld.setStorm(false);
        editWorld.setWeatherDuration(-1);

        Bukkit.getServer().getPluginManager().registerEvents(this, GameAPI.getInstance());
    }

    public ConfigurationSection getTemporaryConfig() {
        return temporaryConfig;
    }

    public void close(boolean save) {
        HandlerList.unregisterAll(this);

        GameAPI.MAP_EDITING_MANAGER.getCurrentlyEditingMaps().remove(editor);
        if (!map.getGameBase().moveToLobby(editor)) {
            editor.getPlayer().kickPlayer(ChatColor.RED + "Attempted to move to a lobby but failed.");
        }

        Bukkit.getServer().unloadWorld(editWorld, save);
        if (save) {
            map.updateOptions(temporaryConfig);
            GameManager.reloadMap(slimeWorld, map);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity().getWorld() == editWorld) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGamemodeChange(PlayerGameModeChangeEvent event) {
        if (!(event.getPlayer().getWorld() == editWorld)) {
            return;
        }
        if (!(event.getNewGameMode().equals(GameMode.SPECTATOR) || event.getNewGameMode().equals(GameMode.CREATIVE))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (GamePlayer.getPlayer(event.getPlayer()) == editor) {
            close(false);
        }
    }

    @EventHandler
    public void worldChangeEvent(PlayerChangedWorldEvent event) {
        if (GamePlayer.getPlayer(event.getPlayer()) == editor) {
            if (event.getFrom().equals(editWorld)) {
                event.getPlayer().sendMessage(ChatColor.RED + "You have changed your world, cancelling all changes.");
                close(false);
            }
        }
    }

    public Map getMap() {
        return map;
    }

    public GamePlayer getEditor() {
        return editor;
    }

    public World getEditWorld() {
        return editWorld;
    }

    public void sendOptionsMessage(boolean all) {
        GameOptionInfo[] options = map instanceof GameMap ? map.getGameBase().getOptionInformation(((GameMap) map).getInstanceCode()) : map.getGameBase().getLobbyManager().getLobbyOptionInformation(((LobbyBase) map));

        if (options.length != 0) {
            editor.getPlayer().sendMessage(" ");
            editor.getPlayer().sendMessage(ChatColor.GRAY + "All options: ");
            for (GameOptionInfo option : options) {
                sendOptionMessage(option, 0, all);
            }
        }
    }

    public void sendOptionMessage(GameOptionInfo option, int depth, boolean all) {
        GameOptionInfo.ValidationState validationState = option.isValid(temporaryConfig, false);

        if (all || !validationState.equals(GameOptionInfo.ValidationState.VALID)) {
            //Made this variable because it's painful to do GamePlayer#getPlayer() every single time.
            Player editorBukkit = editor.getPlayer();

            editorBukkit.sendMessage(("   ".repeat(Math.max(0, depth))) + "- " + getBasedOnValidationState(validationState) + option.getRealDisplayName() + ChatColor.GRAY + (!option.isRequired() ? " (Optional)" : " (Required)"));
            editorBukkit.sendMessage(("   ".repeat(Math.max(0, depth) + 1)) + ChatColor.GRAY + "Type: " + ChatColor.DARK_GRAY + option.getOptionType().displayName);
            editorBukkit.sendMessage(("   ".repeat(Math.max(0, depth) + 1)) + ChatColor.GRAY + "Key: " + ChatColor.DARK_GRAY + option.getKey());
            sendCurrentValue(option, depth);

            if (option.getOptionSpecificInfo() != null) {
                sendOptionSpecificInfo(option, depth, editorBukkit, all);
            }
        }
    }

    private void sendCurrentValue(GameOptionInfo option, int depth) {
        if (option.getOptionType().equals(OptionType.CONFIGURATION_SECTION) || option.getOptionType().equals(OptionType.ARRAY))
            return;

        if (temporaryConfig.contains(option.getKey())) {
            editor.getPlayer().sendMessage(("   ".repeat(Math.max(0, depth) + 1)) + ChatColor.GRAY + "Current Value: " + ChatColor.DARK_GRAY + getValue(option));
        }
    }

    private String getValue(GameOptionInfo option) {
        Object value = temporaryConfig.get(option.getKey());

        if (value instanceof GameLocation location) {
            return "(" + "x: " + location.getX() + " y: " + location.getY() + " z: " + location.getZ() + " [yaw: " + location.getYaw() + " pitch: " + location.getPitch() + "]" + ")";
        }

        return value.toString();
    }

    private void sendOptionSpecificInfo(GameOptionInfo option, int depth, Player editorBukkit, boolean all) {
        if (option.getOptionSpecificInfo() instanceof GameOptionInfo.OptionSpecificInfo.Array array) {
            editorBukkit.sendMessage(("   ".repeat(Math.max(0, depth) + 1)) + ChatColor.GRAY + "Needed length: " + (array.getLength() == 0 ? "Unlimited" : getLength(array, option)) + ", Type: " + array.getType().displayName);
        } else {
            GameOptionInfo.OptionSpecificInfo.ConfigurationSection section = (GameOptionInfo.OptionSpecificInfo.ConfigurationSection) option.getOptionSpecificInfo();

            editorBukkit.sendMessage(("   ".repeat(Math.max(0, depth) + 1)) + ChatColor.GRAY + "Values: ");
            sendSectionMessage(option, section, depth + 1, all);
        }
    }

    private void sendSectionMessage(GameOptionInfo option, GameOptionInfo.OptionSpecificInfo.ConfigurationSection section, int depth, boolean all) {
        for (GameOptionInfo info : section.getOptionInfos()) {
            sendOptionMessage(info, depth + 1, all);
        }
    }

    public String getLength(GameOptionInfo.OptionSpecificInfo.Array array, GameOptionInfo option) {
        ConfigurationSection values = temporaryConfig.getConfigurationSection(option.getKey());

        return (array.getLength() == 0 ? (values == null) ? "0" : String.valueOf(values.getKeys(false).size()) : ((values == null) ? "0/" + array.getLength() : values.getKeys(false).size() + "/" + array.getLength()));
    }

    public ChatColor getBasedOnValidationState(GameOptionInfo.ValidationState state) {
        return switch (state) {
            case INVALID_FROM_CONFIG -> ChatColor.DARK_RED;
            case VALID -> ChatColor.GREEN;
            case INVALID -> ChatColor.RED;
            case PARTIALLY_VALID -> ChatColor.DARK_GREEN;
        };
    }

    public GameOptionInfo.ValidationState areOptionsValid() {
        for (GameOptionInfo info : map instanceof GameMap ? map.getGameBase().getOptionInformation(((GameMap) map).getInstanceCode()) : map.getGameBase().getLobbyManager().getLobbyOptionInformation(((LobbyBase) map))
        ) {
            GameOptionInfo.ValidationState state = info.isValid(temporaryConfig, false);

            if (!info.isRequired()) {
                continue;
            }

            if (state.equals(GameOptionInfo.ValidationState.VALID)) {
                continue;
            }

            return state;
        }

        return GameOptionInfo.ValidationState.VALID;
    }

    public void enter() {
        editor.getPlayer().sendMessage(ChatColor.GREEN + "You are now editing: " + map.getDisplayName());
        GameInstance.normalReset(editor, GameMode.CREATIVE);
        editor.getPlayer().teleport(editWorld.getSpawnLocation());
        editor.getPlayer().setAllowFlight(true);
        editor.getPlayer().setFlying(true);
        sendOptionsMessage(false);
    }

    public void attemptExit() {
        if (!areOptionsValid().equals(GameOptionInfo.ValidationState.VALID)) {
            editor.getPlayer().sendMessage(ChatColor.GRAY + "You may not exit now, some required options are not set.");
            return;
        }

        editor.getPlayer().sendMessage(ChatColor.GREEN + "Exiting...");
        close(true);
    }

    public GameOptionInfo[] getOptions() {
        return map instanceof GameMap ? map.getGameBase().getAdditionalOptionInformation(((GameMap) map).getInstanceCode()) : map.getGameBase().getLobbyManager().getAdditionalLobbyOptionInformation(((LobbyBase) map));
    }

    public GameOptionInfo getOption(String key) {
        GameOptionInfo[] options = map instanceof GameMap ? map.getGameBase().getAdditionalOptionInformation(((GameMap) map).getInstanceCode()) : map.getGameBase().getLobbyManager().getAdditionalLobbyOptionInformation(((LobbyBase) map));

        for (GameOptionInfo info : options) {
            if (info.getKey().equals(key)) {
                return info;
            }
        }

        return null;
    }
}

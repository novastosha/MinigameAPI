package dev.nova.gameapi.game.base.instance;

import com.google.common.collect.ImmutableList;
import dev.nova.gameapi.GameAPI;
import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.formats.chat.Message;
import dev.nova.gameapi.game.base.instance.events.GameEvent;
import dev.nova.gameapi.game.base.instance.controller.GameController;
import dev.nova.gameapi.game.base.instance.events.exception.AlreadyInjectedException;
import dev.nova.gameapi.game.base.instance.formats.tablist.TablistManager;
import dev.nova.gameapi.game.base.instance.values.base.GameValue;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.map.MapInjection;
import dev.nova.gameapi.game.map.options.OptionType;
import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.utils.Titles;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.nova.gameapi.game.base.scoreboard.Scoreboard;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

public abstract class GameInstance {



    public class LayoutManager {


    }

    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    protected final int gameID;

    protected LayoutManager layoutManager;
    protected final GameMap gameMap;

    protected final GameBase gameBase;

    protected final ArrayList<GamePlayer> players;
    protected final ArrayList<GameEvent> events;
    protected final Map<GamePlayer, ArrayList<Block>> blocks;

    protected final GameMap.Map map;
    protected final GameUtils utils;
    protected final String[] description;
    protected final String brief;
    protected final String displayName;
    protected final boolean canBeSpectated;
    protected final Location spectatorLocation;
    protected final ArrayList<GamePlayer> spectators;
    protected final ArrayList<Scoreboard> playerScoreboards;
    protected final GameController[] controllers;
    protected final ArrayList<GameValue<?>> values;
    public final int eventsTaskID;
    private final ArrayList<Location> filter;
    private final boolean rejoinCapable;

    protected GameState gameState;
    protected boolean canStart = true;
    protected boolean ended = false;
    private ArrayList<GameEvent> cloneEvents;
    private GameEvent currentEvent;
    private int currentEventIndex = -1;

    protected Function<Message, String> playersChatFormat = (message -> ChatColor.GRAY + message.player().getPlayer().getName() + ChatColor.RESET + ": " + message.message());
    protected Function<Message, String> spectatorsChatFormat = (message -> ChatColor.GRAY + "[SPECTATOR] " + message.player().getPlayer().getName() + ChatColor.RESET + ": " + message.message());

    protected Function<GamePlayer, String> tablistHeaderFormat = (player -> ChatColor.GRAY + " You are playing: " + player.getGame().displayName);
    protected Function<GamePlayer, String> tablistLowerFormat = (player -> ChatColor.GRAY + " Players Left: " + player.getGame().getPlayers().size());
    protected Function<GamePlayer, String> disconnectionFormat = (player -> ChatColor.GRAY + player.getPlayer().getName() + " left the game.");
    protected Function<GamePlayer, String> rejoiningFormat = (player -> ChatColor.GRAY + player.getPlayer().getName() + " rejoined.");

    public Function<Message, String> getPlayersChatFormat() {
        return playersChatFormat;
    }

    public Function<Message, String> getSpectatorsChatFormat() {
        return spectatorsChatFormat;
    }

    public Function<GamePlayer, String> getTablistHeaderFormat() {
        return tablistHeaderFormat;
    }

    public Function<GamePlayer, String> getTablistLowerFormat() {
        return tablistLowerFormat;
    }

    public boolean rejoin(GamePlayer player) {
        return false;
    }

    public void setTablistHeaderFormat(Function<GamePlayer, String> tablistHeaderFormat) {
        this.tablistHeaderFormat = tablistHeaderFormat;
    }

    public void setTablistLowerFormat(Function<GamePlayer, String> tablistLowerFormat) {
        this.tablistLowerFormat = tablistLowerFormat;
    }

    public boolean isTablistFormatNull() {
        return tablistHeaderFormat == null || tablistLowerFormat == null;
    }

    protected void setPlayersChatFormat(Function<Message, String> playersChatFormat) {
        this.playersChatFormat = playersChatFormat;
    }

    protected void setSpectatorChatFormat(Function<Message, String> spectatorsChatFormat) {
        this.spectatorsChatFormat = spectatorsChatFormat;
    }

    /**
     * {@inheritDoc}
     *
     * @param map The map the players are going to play in.
     */
    protected GameInstance(String displayName, String gameBase, GameMap map, String[] gameDescription, String gameBrief, boolean canBeSpectated, GameController[] gameControllers, boolean rejoinCapable) {
        this.gameBase = GameManager.getGame(gameBase);
        this.canBeSpectated = canBeSpectated;
        this.rejoinCapable = rejoinCapable;
        this.values = new ArrayList<GameValue<?>>();
        this.spectators = new ArrayList<GamePlayer>();
        this.displayName = displayName;
        this.layoutManager = new LayoutManager();
        this.description = gameDescription;
        this.brief = gameBrief;
        this.filter = new ArrayList<Location>();
        this.blocks = new HashMap<>();
        this.playerScoreboards = new ArrayList<>();
        this.controllers = gameControllers;
        this.gameID = this.gameBase.getRunningInstances().get(this.gameBase.getCode(getClass())) != null ?
                this.gameBase.getRunningInstances().get(this.gameBase.getCode(getClass())).size() + 1
                : this.gameBase.getRunningInstances().size() + 1;
        this.gameMap = map;
        this.events = new ArrayList<>();
        this.players = new ArrayList<GamePlayer>();

        this.map = map != null ? map.newMap(this) : null;

        if (this.map != null)
            Arrays.stream(this.map.getCustomInjections().toArray(new MapInjection[0])).forEach(MapInjection::onInitialize);


        this.gameState = GameState.CLOSED;

        this.utils = new GameUtils();

        this.spectatorLocation = canBeSpectated ? this.map.loadOption("spectator-location", OptionType.LOCATION, true).getAsLocation() : null;

        if (cloneEvents != null) {
            this.eventsTaskID = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(GameAPI.getPlugin(GameAPI.class), new Runnable() {
                @Override
                public void run() {
                    ArrayList<GameEvent> toRemove = new ArrayList<>();

                    for (GameEvent event : cloneEvents) {
                        event.durationLeft--;

                        if (event.durationLeft == 0) {
                            currentEvent = event;
                            toRemove.add(event);
                            currentEvent.onEvent();
                            currentEventIndex++;
                        }
                    }

                    cloneEvents.removeAll(toRemove);
                    toRemove.clear();
                }
            }, 0L, 20L);
        } else {
            this.eventsTaskID = -1;
        }
    }

    public int getCurrentEventIndex() {
        return currentEventIndex;
    }

    public ArrayList<GameValue<?>> getValues() {
        return values;
    }

    protected void addValue(GameValue<?> value) {
        this.values.add(value);
    }

    /**
     * This is unsafe to use!
     */
    @SuppressWarnings("unchecked")
    protected <T> GameValue<T> getValue(String id) {
        for (GameValue<?> value : values) {
            if (value.getId().equalsIgnoreCase(id)) return (GameValue<T>) value;
        }
        return null;
    }

    protected GameValue<?> getSafeValue(String id) {
        for (GameValue<?> value : values) {
            if (value.getId().equalsIgnoreCase(id)) return value;
        }
        return null;
    }

    public GameController[] getControllers() {
        return controllers;
    }

    public boolean containsController(GameController gameController) {
        for (GameController controller : controllers) {
            if (controller.equals(gameController)) return true;
        }
        return false;
    }

    public ArrayList<GamePlayer> getSpectators() {
        return spectators;
    }

    public boolean canBeSpectated() {
        return canBeSpectated;
    }

    public Location getSpectatorLocation() {
        return spectatorLocation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBrief() {
        return brief;
    }

    public String[] getDescription() {
        return description;
    }

    public int getGameID() {
        return gameID;
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    public GameBase getGameBase() {
        return gameBase;
    }

    public ArrayList<GamePlayer> getPlayers() {
        return players;
    }

    public boolean hasEnded() {
        return ended;
    }

    /**
     * Called when a player joins the game.
     *
     * @param player The player that joined.
     */
    public void join(GamePlayer player) {
        if (canStart) {
            players.add(player);
            player.setGame(this);
            player.setPreviousGame(this);
            for (GamePlayer send : players) {
                send.getPlayer().sendMessage(player.getPlayer().getName() + " §a joined the game! §7(" + players.size() + "/" + gameMap.getPlayerLimit() + ")");
            }
        } else {
            player.getPlayer().sendMessage("§cYou cannot join this game!");
        }
    }

    public void onStart() {
        if (canStart) {
            this.cloneEvents = new ArrayList<>(events);
            this.gameState = GameState.STARTED;
            if (gameMap != null) {
                for (MapInjection injection : map.getCustomInjections()) {
                    injection.onStart();
                }
            }

            utils.sendTitle(gameBase.getGameTheme() + "§l" + gameBase.getDisplayName(), ChatColor.GRAY + brief, 10, 10, 18);

            for (String descLine : description) {
                utils.sendMessage("§e§l" + descLine);
            }
        } else {
            utils.sendMessage("§cUnable to start game!");
            onEnd(true);
        }
    }

    public boolean isRejoinCapable() {
        return rejoinCapable;
    }

    public enum EndState {
        DO_NOTHING,
        IMMEDIATE,
        FAILED,
        SUCCESS
    }

    public EndState onEnd(boolean immediate) {

        if(!GameAPI.getPlugin(GameAPI.class).getConfig().getBoolean("after-end.enabled",false)){
            immediate = true;
        }

        if (!ended) {
            this.gameState = GameState.ENDED;
            this.ended = true;
            if (gameMap != null) {
                for (MapInjection injection : map.getCustomInjections()) {
                    injection.onEnd();
                }
            }

            boolean failed = false;

            for (GamePlayer player : players) {
                TablistManager.resetTablist(player);
                player.setGame(null);
                player.setPreviousGame(null);
                player.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                try {
                    if (!failed) {
                        failed = !resetPlayer(player);
                    }
                }catch (Exception e) {
                    failed = true;
                }
                normalReset(player,GameMode.ADVENTURE);

                if(immediate || failed) {
                    gameBase.moveToLobby(player);
                }
            }

            for (GamePlayer player : spectators) {
                TablistManager.resetTablist(player);
                player.setGame(null);
                player.setPreviousGame(null);
                player.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                try {
                    if (!failed) {
                        failed = !resetSpectator(player);
                    }
                }catch (Exception e) {
                    failed = true;
                }
                normalReset(player,GameMode.SPECTATOR);

                if(immediate || failed) {
                    gameBase.moveToLobby(player);
                }
            }

            if(failed || immediate) {
                deleteMap();
            }

            playerScoreboards.clear();
            this.blocks.clear();
            this.events.clear();
            this.cloneEvents.clear();
            this.values.clear();
            Bukkit.getServer().getScheduler().cancelTask(eventsTaskID);

            if(failed || immediate){
                players.clear();
                spectators.clear();
                deleteMap();
                this.gameBase.removeInstance(this);
            }else{
                setGameState(GameState.AFTER_END);
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        setGameState(GameState.ENDED);
                        players.forEach(gameBase::moveToLobby);
                        spectators.forEach(gameBase::moveToLobby);
                        deleteMap();
                        players.clear();
                        spectators.clear();
                        gameBase.removeInstance(GameInstance.this);
                    }
                }.runTaskLater(GameAPI.getPlugin(GameAPI.class),(GameAPI.getPlugin(GameAPI.class).getConfig().getInt("after-end.duration",28))*20L);
            }

            return failed ? EndState.FAILED : immediate ? EndState.IMMEDIATE : EndState.SUCCESS;
        }else{
            return EndState.DO_NOTHING;
        }
    }

    /**
     *
     * FIXME: Needs heavy re-work.
     *
     */
    public static abstract class NameOption {

        protected static String[] splitString16(String string) {

            String firstPart = string.substring(0,15);
            String thirdPart = string.replaceFirst(firstPart,"");


            return new String[] {
                    firstPart,
                    thirdPart
            };
        }

        protected static String[] splitString48(String string) {

            String firstPart = string.substring(0,15);
            String secondPart = string.substring(15,30);
            String thirdPart = string.replaceFirst(firstPart,"").replaceFirst(secondPart,"");


            return new String[] {
                    firstPart,
                    secondPart,
                    thirdPart
            };
        }

        protected final String data;

        private NameOption(String data) {
            this.data = data;
        }

        public abstract void update(GamePlayer player);
        public abstract void reset(GamePlayer player);

        public static class NAMETAG extends NameOption {

            private String teamName;

            public NAMETAG(String tag) {
                super(tag);
            }

            @Override
            public void update(GamePlayer player) {

                player.getPlayer().setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());

                String prefix = "";
                String name = data;
                String suffix = "";

                if(data.length() > 16 && data.length() <= 32) {
                    String[] split = splitString16(data);

                    prefix = split[0];
                    name = split[1];
                }else if(data.length() > 32 && data.length() <= 48){
                    String[] split = splitString48(data);

                    prefix = split[0];
                    name = split[1];
                    suffix = split[2];
                }

                if(teamName != null){
                    player.getPlayer().getScoreboard().getTeam(teamName).unregister();
                }

                teamName = UUID.randomUUID().toString().split("-")[0];
                player.getPlayer().getScoreboard().registerNewTeam(teamName);

                Team team = player.getPlayer().getScoreboard().getTeam(teamName);

                team.setDisplayName(name);
                if(!suffix.isBlank()){
                    team.setPrefix(prefix);
                }
                if (!suffix.isBlank()) {
                    team.setSuffix(suffix);
                }

                team.addPlayer(player.getPlayer());
            }

            @Override
            public void reset(GamePlayer player) {
                if(teamName == null) return;

                Team team = player.getPlayer().getScoreboard().getTeam(teamName);
                if(team == null) return;

                team.unregister();

            }
        }

    }

    /**
     * Kicks players, deletes the world
     */
    private void deleteMap() {
        if (map != null) {
            if (map.getBukkitWorld().getPlayers().size() != 0) {
                for (Player player : map.getBukkitWorld().getPlayers()) {
                    player.kickPlayer("§cGame ended and you were not moved to a lobby!");
                }
            }
            Bukkit.unloadWorld(map.getBukkitWorld(), false);
        }
    }

    protected abstract boolean resetSpectator(GamePlayer player);

    /**
     *
     * Basically became a utility method.
     *
     */
    public static void normalReset(GamePlayer player,GameMode gameMode) {
        Player bukkitPlayer = player.getPlayer();

        bukkitPlayer.setFireTicks(0);
        bukkitPlayer.setFallDistance(0);
        bukkitPlayer.setGameMode(gameMode);
        bukkitPlayer.setExp(0);
        bukkitPlayer.setLevel(0);
        bukkitPlayer.setTotalExperience(0);
        bukkitPlayer.setSaturation(200);
        bukkitPlayer.setHealth(20);
        bukkitPlayer.getInventory().clear();
        bukkitPlayer.setFoodLevel(20);
        for (PotionEffect effect : bukkitPlayer.getActivePotionEffects()) {
            bukkitPlayer.removePotionEffect(effect.getType());
        }
    }

    public Scoreboard getScoreboard(GamePlayer player) {
        for (Scoreboard scoreboard : playerScoreboards) {
            if (scoreboard.getPlayers().contains(player.getPlayer())) {
                return scoreboard;
            }
        }
        return null;
    }

    public void addScoreboard(GamePlayer player, Scoreboard scoreboard) {
        playerScoreboards.add(scoreboard);
        scoreboard.addPlayer(player.getPlayer());
    }

    public void end(String reason) {
        GameLogger.log(new GameLog(gameBase, LogLevel.ERROR, "Instance: " + gameID + " ended unexpectedly because " + reason, true));
        if (!hasEnded()) {
            utils.sendMessage("§cSorry, an unexpected error happened!");
            canStart = false;
            this.gameBase.removeInstance(this);
            onEnd(true);
        }
    }

    public void addEvent(GameEvent event) {
        boolean injected = false;
        try {
            event.injectEvent(this);
        } catch (AlreadyInjectedException e) {
            e.printStackTrace();
            injected = true;
        }

        if (!injected) {
            this.events.add(event);
        }
    }

    public void leave(GamePlayer player) {


        player.getPlayer().sendMessage("§cYou left the game!");
        player.setGame(null);


        if(!gameBase.moveToLobby(player)){
            player.getPlayer().kickPlayer(ChatColor.RED+"There was no lobby to move you to!");
        }

        if (players.contains(player)) {
            onLeavePlayer(player);
        } else {
            onLeaveSpectator(player);
        }

        players.remove(player);
        spectators.remove(player);

        player.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());


    }


    protected void onLeavePlayer(GamePlayer player) {
    }


    protected void onLeaveSpectator(GamePlayer player) {
    }

    /**
     * @return Copy of original events list.
     */
    public List<GameEvent> getEvents() {
        return Arrays.asList(Arrays.copyOf(events.toArray(new GameEvent[0]), events.size()));
    }


    public GameMap.Map getMap() {
        return map;
    }

    public GameState getGameState() {
        return gameState;
    }

    protected void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    /**
     * Gets called every server tick.
     */
    public abstract void tick();

    public GameUtils getUtils() {
        return utils;
    }

    public ArrayList<Scoreboard> getPlayerScoreboards() {
        return playerScoreboards;
    }

    public void joinSpectator(GamePlayer player) {
        if (spectatorLocation != null) {
            player.getPlayer().teleport(spectatorLocation);
            switchToSpectator(player);
            getUtils().sendMessage("§7" + player.getPlayer().getName() + " started spectating!");
        } else {
            player.getPlayer().sendMessage("§cCannot spectate!");
        }
    }

    public void onValueUpdate(GameValue<?> tGameValue) {

    }

    public void leave(GamePlayer player, boolean b) {

        if (players.contains(player)) {
            onLeavePlayer(player);
        } else {
            onLeaveSpectator(player);
        }

        if (b) {
            players.remove(player);
            spectators.remove(player);
        }

        player.getPlayer().sendMessage("§cYou left the game!");
        player.setGame(null);
        if (!hasEnded()) {
            gameBase.moveToLobby(player);
        }
    }

    public GameEvent getCurrentEvent() {
        return currentEvent;
    }

    public Function<Message, String> getChatFormat(GamePlayer player) {
        return spectators.contains(player) ? getSpectatorsChatFormat() : getPlayersChatFormat();
    }

    public void handleChatEvent(AsyncPlayerChatEvent event) {

        GamePlayer player = GamePlayer.getPlayer(event.getPlayer());

        String message = player.getGame().getChatFormat(player).apply(new Message(player, event.getMessage()));
        player.getGame().getUtils().sendMessage(message);
        event.setCancelled(true);
    }

    public class GameUtils {

        private GameUtils() {
        }

        public void sendMessage(String message) {
            Arrays.stream(players.toArray(new GamePlayer[0])).forEach(gamePlayer -> {
                gamePlayer.getPlayer().sendMessage(message);
            });
            Arrays.stream(spectators.toArray(new GamePlayer[0])).forEach(gamePlayer -> {
                gamePlayer.getPlayer().sendMessage(message);
            });
        }

        public void sendTitle(String title, String subTitle, int fadeIn, int stay, int fadeOut) {
            Arrays.stream(players.toArray(new GamePlayer[0])).forEach(gamePlayer -> {
                Titles.sendTitle(gamePlayer.getPlayer(),fadeIn,stay,fadeOut,title,subTitle);
            });
            Arrays.stream(spectators.toArray(new GamePlayer[0])).forEach(gamePlayer -> {
                Titles.sendTitle(gamePlayer.getPlayer(),fadeIn,stay,fadeOut,title,subTitle);
            });
        }

        public enum CountDownType {
            PER_SECOND,
            SINGLE
        }

        /**
         * This will reset the player's XP
         *
         * @param player  The player.
         * @param seconds For how long
         * @param type    The way to display the Countdown
         * @param finish  Something to run when the countdown finishes
         */
        public void countdown(GamePlayer player, int seconds, CountDownType type, Consumer<GamePlayer> finish) {
            if (!players.contains(player)) {
                return;
            }

            player.getPlayer().setLevel(seconds);
            player.getPlayer().setExp(1);

            final int[] secs = {seconds};

            final float toRemove = 0.5F / (seconds/2F);

            final float[] exp = {1};

            new BukkitRunnable() {

                @Override
                public void run() {
                    if(type.equals(CountDownType.PER_SECOND)){
                        if(exp[0] <= 0){
                            exp[0] = 1;
                        }
                    }

                    secs[0]--;
                    player.getPlayer().setLevel(secs[0]);

                    if (type.equals(CountDownType.SINGLE)) {
                        player.getPlayer().setExp(0);
                    }else{
                        exp[0] -=toRemove;
                        if(exp[0] <= 0){
                            exp[0] = 1;
                        }
                        player.getPlayer().setExp(exp[0]);
                    }

                    if(secs[0] == 0){
                        cancel();
                        player.getPlayer().setExp(0);

                        if(finish != null) finish.accept(player);
                    }
                }
            }.runTaskTimer(GameAPI.getPlugin(GameAPI.class),0,20L);


        }

        public void countdown(GamePlayer player, int seconds, CountDownType type) {
            countdown(player, seconds, type, null);
        }

    }

    public void afterEnd(Event event){
        GamePlayer player = null;
        if (event instanceof PlayerChangedWorldEvent worldChangeEvent) {

            if (map != null) {
                if (worldChangeEvent.getFrom().equals(map.getBukkitWorld())) {
                    player = GamePlayer.getPlayer(worldChangeEvent.getPlayer());
                }
            }

        } else if (event instanceof PlayerQuitEvent) {
            player = GamePlayer.getPlayer(((PlayerQuitEvent) event).getPlayer());
        }

        if (player != null) {
            if (players.contains(player) || spectators.contains(player)) {
                players.remove(player);
                spectators.remove(player);
                TablistManager.resetTablist(player);
                player.setGame(null);
                player.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }

        if(event instanceof PlayerMoveEvent moveEvent){
            if(moveEvent.getTo().getBlockY() <= 1){
                player = GamePlayer.getPlayer(moveEvent.getPlayer());
                if(players.contains(player)){
                    resetPlayer(player);
                }else{
                    resetSpectator(player);
                }
                normalReset(GamePlayer.getPlayer(moveEvent.getPlayer()),players.contains(player) ? GameMode.ADVENTURE : GameMode.SPECTATOR);
            }
        }

        if(event instanceof Cancellable cancellableEvent && !(event instanceof PlayerMoveEvent) && !(event instanceof PlayerGameModeChangeEvent)){
            cancellableEvent.setCancelled(true);
        }
    }

    public void onEvent(Event event) {
        if (event instanceof BlockPlaceEvent) {
            if (!containsController(GameController.PLACE_BLOCKS)) {
                ((Cancellable) event).setCancelled(true);
            } else {
                GamePlayer player = GamePlayer.getPlayer(((BlockPlaceEvent) event).getPlayer());
                ArrayList<Block> blocks = this.blocks.get(player) != null ? this.blocks.get(player) : new ArrayList<>();

                blocks.add(((BlockPlaceEvent) event).getBlock());
                this.blocks.put(player, blocks);
            }
        }
        if (event instanceof BlockBreakEvent) {
            GamePlayer player = GamePlayer.getPlayer(((BlockBreakEvent) event).getPlayer());

            boolean contains = false;

            for (ArrayList<Block> blocksA : this.blocks.values()) {
                if (blocksA.contains(((BlockBreakEvent) event).getBlock())) {
                    contains = true;
                    break;
                }
            }

            Location location = ((BlockBreakEvent) event).getBlock().getLocation();
            location.setWorld(getMap().getBukkitWorld());

            if (!contains && !containsController(GameController.BREAK_BLOCKS_MAP) && !filter.contains(location)) {
                ((Cancellable) event).setCancelled(true);
                player.getPlayer().sendMessage("§cYou cannot break map blocks!");
            }

            if (this.blocks.get(player) != null && this.blocks.get(player).contains(((BlockBreakEvent) event).getBlock()) && !containsController(GameController.BREAK_BLOCKS)) {
                ((Cancellable) event).setCancelled(true);
            }
        }

        if (event instanceof PlayerPickupItemEvent pickupItemEvent && !containsController(GameController.ITEM_PICKUP)) {
            pickupItemEvent.setCancelled(true);
        }

        if (event instanceof PlayerDropItemEvent && !containsController(GameController.ITEM_DROP)) {
            ((Cancellable) event).setCancelled(true);
        }

        if (event instanceof PlayerPickupItemEvent && !containsController(GameController.ARROW_PICKUPS)) {
            if(((PlayerPickupItemEvent) event).getItem().getType().equals(EntityType.ARROW)) {
                ((Cancellable) event).setCancelled(true);
            }
        }

        if (event instanceof PlayerDeathEvent deathEvent) {

            if (!containsController(GameController.DEATH_DROPS)) {
                deathEvent.setDroppedExp(0);
                deathEvent.getDrops().clear();

            }
        }

        if (event instanceof WeatherChangeEvent && !containsController(GameController.WEATHER)) {
            ((Cancellable) event).setCancelled(true);
            return;
        }

        GamePlayer player = null;
        if (event instanceof PlayerChangedWorldEvent worldChangeEvent) {

            if (map != null) {
                if (worldChangeEvent.getFrom().equals(map.getBukkitWorld())) {
                    player = GamePlayer.getPlayer(worldChangeEvent.getPlayer());
                }
            }

        } else if (event instanceof PlayerQuitEvent) {
            player = GamePlayer.getPlayer(((PlayerQuitEvent) event).getPlayer());
        }

        if (player != null) {
            if (spectators.contains(player)) {
                getUtils().sendMessage("§7" + player.getPlayer().getName() + " is no longer spectating!");
            } else if (players.contains(player)) {
                getUtils().sendMessage(disconnectionFormat.apply(player));
            }
            if (players.contains(player) || spectators.contains(player)) {
                players.remove(player);
                spectators.remove(player);
                player.getPlayer().sendMessage("§7You left the game with id: " + gameID);
                player.setGame(null);
                player.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                TablistManager.resetTablist(player);
            }
        }
    }

    public ArrayList<Location> getFilter() {
        return filter;
    }

    public void switchToSpectator(GamePlayer player) {
        Bukkit.getServer().getScheduler().runTaskLater(GameAPI.getPlugin(GameAPI.class), new Runnable() {
            @Override
            public void run() {
                player.getPlayer().spigot().respawn();
            }
        }, 5L);
        players.remove(player);
        spectators.add(player);
        player.getPlayer().setGameMode(GameMode.SPECTATOR);
        player.getPlayer().setHealth(20L);
        player.getPlayer().setFoodLevel(20);
        player.getPlayer().getInventory().clear();
        player.getPlayer().sendMessage(ChatColor.GRAY + "You are now a spectator!");
    }

    /**
     *
     * If false was returned the game will immediately end.
     *
     * @return Whether the reset failed or not.
     */
    public abstract boolean resetPlayer(GamePlayer player);
}

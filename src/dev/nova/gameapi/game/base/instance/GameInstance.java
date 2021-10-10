package dev.nova.gameapi.game.base.instance;

import dev.nova.gameapi.GAPIPlugin;
import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.GameEvent;
import dev.nova.gameapi.game.base.instance.controller.GameController;
import dev.nova.gameapi.game.base.scoreboard.Scoreboard;
import dev.nova.gameapi.game.base.scoreboard.game.GameScoreboard;
import dev.nova.gameapi.game.base.scoreboard.player.PlayerScoreboard;
import dev.nova.gameapi.game.base.values.GameValue;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.map.MapInjection;
import dev.nova.gameapi.game.map.options.OptionType;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.*;

public abstract class GameInstance {

    private final int gameID;

    private final GameMap gameMap;

    private final GameBase gameBase;

    private final ArrayList<GamePlayer> players;
    private final ArrayList<GameEvent> events;
    private final ArrayList<GameValue<?>> values;
    private final Map<GamePlayer,ArrayList<Block>> blocks;

    private final GameMap.Map map;
    private final GameUtils utils;
    private final String[] description;
    private final String brief;
    private final String displayName;
    private final boolean canBeSpectated;
    private final Location spectatorLocation;
    private final ArrayList<GamePlayer> spectators;
    private final ArrayList<PlayerScoreboard> playerScoreboards;
    private final GameController[] controllers;

    private Scoreboard scoreboard;

    private GameState gameState;
    protected boolean canStart = true;
    private boolean ended = false;

    /**
     *
     * {@inheritDoc}
     * 
     * @param map The map the players are going to play in.
     */
    public GameInstance(String displayName,@Nonnull String gameBase, GameMap map,String[] gameDescription, String gameBrief,boolean canBeSpectated, GameController[] gameControllers) {
        this.gameBase = GameManager.getGame(gameBase);
        this.canBeSpectated = canBeSpectated;
        this.spectators = new ArrayList<GamePlayer>();
        this.displayName = displayName;
        this.description =gameDescription;
        this.brief = gameBrief;
        this.blocks = new HashMap<>();
        this.playerScoreboards = new ArrayList<>();
        this.controllers = gameControllers;
        this.gameID = this.gameBase.getRunningInstances().get(this.gameBase.getCode(getClass())) != null ?
                this.gameBase.getRunningInstances().get(this.gameBase.getCode(getClass())).size() +1
                : this.gameBase.getRunningInstances().size()+1;
        this.gameMap = map;
        this.events = new ArrayList<>();
        this.players = new ArrayList<GamePlayer>();
        values = new ArrayList<>();

        this.map = map.newMap(this);

        this.gameState = GameState.CLOSED;

        this.utils = new GameUtils(this);

        this.spectatorLocation = canBeSpectated ? this.map.loadOption("spectator-location", OptionType.LOCATION,true).getAsLocation() : null;
    }

    protected void setGameScoreboard(GameScoreboard scoreboard){
        this.scoreboard = scoreboard;
    }

    protected void addPlayerScoreboard(PlayerScoreboard scoreboard){
        if(!players.contains(scoreboard.getPlayer()) && !spectators.contains(scoreboard.getPlayer())){
            return;
        }

        playerScoreboards.add(scoreboard);
    }

    public GameController[] getControllers() {
        return controllers;
    }

    public boolean containsController(GameController gameController){
        for(GameController controller : controllers){
            if(controller.equals(gameController)) return true;
        }
        return false;
    }

    public ArrayList<GamePlayer> getSpectators() {
        return spectators;
    }

    public boolean canBeSpectated(){
        return canBeSpectated;
    }

    @Nullable
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

    public boolean hasEnded(){
        return ended;
    }

    /**
     *
     * Called when a player joins the game.
     *
     * @param player The player that joined.
     */
    @OverridingMethodsMustInvokeSuper
    public void join(GamePlayer player){
        if(canStart) {
            players.add(player);
            player.setGame(this);
            for (GamePlayer send : players) {
                send.getPlayer().sendMessage(player.getPlayer().getName() + " §a joined the game! §7(" + players.size() + "/" + gameMap.getPlayerLimit() + ")");
            }
        }else{
            player.getPlayer().sendMessage("§cYou cannot join this game!");
        }
    }

    @OverridingMethodsMustInvokeSuper
    public void onStart(){
        if(canStart) {
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
        }else{
            utils.sendMessage("§cUnable to start game!");
            onEnd();
        }
    }

    @OverridingMethodsMustInvokeSuper
    public void onEnd(){
        if(!ended) {
            this.gameState = GameState.ENDED;
            this.ended = true;
            if (gameMap != null) {
                for (MapInjection injection : map.getCustomInjections()) {
                    injection.onEnd();
                }
            }

            for(GamePlayer player : players)  {
                player.setGame(null);
                player.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

            }

            for(GamePlayer player : spectators){
                player.setGame(null);
                player.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }

            playerScoreboards.clear();

            if(map != null ){
                /*if(map.getBukkitWorld().getPlayers().size() != 0) {
                    for (Player player : map.getBukkitWorld().getPlayers()){
                        player.kick(Component.text("§cGame ended and you were not moved to a lobby / a fallback server!"));
                    }
                    Bukkit.unloadWorld(map.getBukkitWorld(),false);
                    map.deleteWorld(map.getBukkitWorld().getWorldFolder());
                }*/
            }

            //this.gameBase.removeInstance(this);
        }
    }

    public void postEnd(){
        canStart = false;
        this.gameBase.removeInstance(this);
        onEnd();
    }

    @OverridingMethodsMustInvokeSuper
    public void addEvent(GameEvent event){
        this.events.add(event);
    }

    @OverridingMethodsMustInvokeSuper
    protected void addValue(GameValue<?> gameValue){
        this.values.add(gameValue);
    }

    @OverridingMethodsMustInvokeSuper
    public void leave(GamePlayer player){

        if(players.contains(player)){
            onLeavePlayer(player);
        }else{
            onLeaveSpectator(player);
        }

        players.remove(player);
        spectators.remove(player);

        player.getPlayer().sendMessage("§cYou left the game!");
        player.setGame(null);
    }


    protected void onLeavePlayer(GamePlayer player) {
    }


    protected void onLeaveSpectator(GamePlayer player) {
    }

    /**
     *
     * @return Copy of original events list.
     */
    public List<GameEvent> getEvents() {
        return Arrays.asList(Arrays.copyOf(events.toArray(new GameEvent[0]),events.size()));
    }

    /**
     *
     * @return Copy of original values list.
     */
    public List<GameValue<?>> getValues() {
        return Arrays.asList(Arrays.copyOf(values.toArray(new GameValue<?>[0]),values.size()));
    }

    public void onValueUpdate(GameValue<?> gameValue) {

    }

    @Nullable
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

    public Scoreboard getGameScoreboard() {
        return scoreboard;
    }

    public ArrayList<PlayerScoreboard> getPlayerScoreboards() {
        return playerScoreboards;
    }

    @OverridingMethodsMustInvokeSuper
    public void joinSpectator(GamePlayer player) {
        if (spectatorLocation != null) {
            player.getPlayer().teleport(spectatorLocation);
            switchToSpectator(player);
            getUtils().sendMessage("§7" + player.getPlayer().getName() + " started spectating!");
        }else{
            player.getPlayer().sendMessage("§cCannot spectate!");
        }
    }

    public static class GameUtils {

        private final GameInstance instance;

        private GameUtils(GameInstance instance){
            this.instance = instance;
        }

        public void sendMessage(String message){
            Arrays.stream(instance.players.toArray(new GamePlayer[0])).forEach(gamePlayer -> {gamePlayer.getPlayer().sendMessage(message);});
            Arrays.stream(instance.spectators.toArray(new GamePlayer[0])).forEach(gamePlayer -> {gamePlayer.getPlayer().sendMessage(message);});
        }

        public void sendTitle(String title, String subTitle,int i, int i1, int i2){
            Arrays.stream(instance.players.toArray(new GamePlayer[0])).forEach(gamePlayer -> {gamePlayer.getPlayer().sendTitle(title, subTitle, i, i1, i2);});
            Arrays.stream(instance.spectators.toArray(new GamePlayer[0])).forEach(gamePlayer -> {gamePlayer.getPlayer().sendTitle(title, subTitle, i, i1, i2);});
        }

    }

    @OverridingMethodsMustInvokeSuper
    public void onEvent(Event event){

        if(event instanceof BlockPlaceEvent){
            if(!containsController(GameController.PLACE_BLOCKS)) {
                ((Cancellable) event).setCancelled(true);
            }else{
                GamePlayer player = GamePlayer.getPlayer(((BlockPlaceEvent) event).getPlayer());
                ArrayList<Block> blocks = this.blocks.get(player) != null ? this.blocks.get(player) : new ArrayList<>();

                blocks.add(((BlockPlaceEvent) event).getBlock());
                this.blocks.put(player,blocks);
            }
        }
        if(event instanceof BlockBreakEvent){
            GamePlayer player = GamePlayer.getPlayer(((BlockBreakEvent) event).getPlayer());

            boolean contains = false;

            for( ArrayList<Block> blocksA : this.blocks.values()){
                if(blocksA.contains(((BlockBreakEvent) event).getBlock())){
                    contains = true;
                    break;
                }
            }

            if(!contains && !containsController(GameController.BREAK_BLOCKS_MAP)){
                ((Cancellable) event).setCancelled(true);
                player.getPlayer().sendMessage("§cYou cannot break map blocks!");
            }

            if(this.blocks.get(player) != null && this.blocks.get(player).contains(((BlockBreakEvent) event).getBlock()) && !containsController(GameController.BREAK_BLOCKS)){
                ((Cancellable) event).setCancelled(true);
            }
        }

        if(event instanceof EntityPickupItemEvent && !containsController(GameController.ITEM_PICKUP)){
            EntityPickupItemEvent pickupItemEvent = (EntityPickupItemEvent) event;
            pickupItemEvent.setCancelled(true);
        }

        if(event instanceof PlayerDropItemEvent && !containsController(GameController.ITEM_DROP)){
            ((Cancellable) event).setCancelled(true);
        }

        if(event instanceof PlayerPickupArrowEvent && !containsController(GameController.ARROW_PICKUPS)){
            ((Cancellable) event).setCancelled(true);
        }

        if(event instanceof PlayerDeathEvent){
            PlayerDeathEvent deathEvent = (PlayerDeathEvent) event;

            if(!containsController(GameController.DEATH_DROPS)) {
                deathEvent.setDroppedExp(0);
                deathEvent.getDrops().clear();

            }
        }

        if(event instanceof WeatherChangeEvent && !containsController(GameController.WEATHER)){
            ((Cancellable) event).setCancelled(true);
            return;
        }

        GamePlayer player = null;
        if(event instanceof PlayerChangedWorldEvent) {

            PlayerChangedWorldEvent worldChangeEvent = (PlayerChangedWorldEvent) event;

            if(map != null) {
                if (worldChangeEvent.getFrom().equals(map.getBukkitWorld())) {
                    player = GamePlayer.getPlayer(worldChangeEvent.getPlayer());
                }
            }
            
        }else if(event instanceof PlayerQuitEvent) {
            player = GamePlayer.getPlayer(((PlayerQuitEvent) event).getPlayer());
        }

        if(player != null) {
            if(spectators.contains(player)){
                getUtils().sendMessage("§7"+player.getPlayer().getName()+" is no longer spectating!");
            }else if(players.contains(player)){
                getUtils().sendMessage("§7"+player.getPlayer().getName()+" left the game!");
            }
            if (players.contains(player) || spectators.contains(player)) {
                players.remove(player);
                spectators.remove(player);
                player.getPlayer().sendMessage("§7You left the game with id: " + gameID);
                player.setGame(null);
            }
        }
    }

    public void switchToSpectator(GamePlayer player){
            Bukkit.getServer().getScheduler().runTaskLater(GAPIPlugin.getPlugin(GAPIPlugin.class), new Runnable() {
                @Override
                public void run() {
                    player.getPlayer().spigot().respawn();
                }
            },5L);
            players.remove(player);
            spectators.add(player);
            player.getPlayer().setGameMode(GameMode.SPECTATOR);
            player.getPlayer().setHealth(20L);
            player.getPlayer().setFoodLevel(20);
            player.getPlayer().sendMessage(ChatColor.GRAY+"You are now a spectator!");
    }
}

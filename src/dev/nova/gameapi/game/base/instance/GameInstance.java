package dev.nova.gameapi.game.base.instance;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.GameEvent;
import dev.nova.gameapi.game.base.instance.controller.GameController;
import dev.nova.gameapi.game.base.scoreboard.Scoreboard;
import dev.nova.gameapi.game.base.values.GameValue;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.map.MapInjection;
import dev.nova.gameapi.game.player.GamePlayer;
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
    private final boolean spectatable;
    private final Location spectatorLocation;
    private final ArrayList<GamePlayer> spectators;
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
    public GameInstance(String displayName,@Nonnull String gameBase, GameMap map,String[] gameDescription, String gameBrief,boolean spectatable, GameController[] gameControllers) {
        this.gameBase = GameManager.getGame(gameBase);
        this.spectatable = spectatable;
        this.spectators = new ArrayList<GamePlayer>();
        this.displayName = displayName;
        this.description =gameDescription;
        this.brief = gameBrief;
        this.blocks = new HashMap<>();
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

        //this.spectatorLocation = spectatable ? this.map.loadOption("spectator-location", OptionType.LOCATION,true).getAsLocation() : null;
        this.spectatorLocation = null;
    }

    protected void setScoreboard(Scoreboard scoreboard){
        this.scoreboard = scoreboard;
        for(GamePlayer player : players){
            getScoreboard().addPlayer(player);
        }
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

    public boolean isSpectatable() {
        return spectatable;
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
            this.gameBase.removeInstance(this);
        }
    }

    public void postEnd(){
        canStart = false;
        this.gameBase.removeInstance(this);
    }

    @OverridingMethodsMustInvokeSuper
    public void addEvent(GameEvent event){
        this.events.add(event);
    }

    @OverridingMethodsMustInvokeSuper
    protected void addValue(GameValue<?> gameValue){
        this.values.add(gameValue);
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

    public Scoreboard getScoreboard() {
        return scoreboard;
    }
    
    public static class GameUtils {

        private final GameInstance instance;

        private GameUtils(GameInstance instance){
            this.instance = instance;
        }

        public void sendMessage(String message){
            Arrays.stream(instance.players.toArray(new GamePlayer[0])).forEach(gamePlayer -> {gamePlayer.getPlayer().sendMessage(message);});
        }

        public void sendTitle(String title, String subTitle,int i, int i1, int i2){
            Arrays.stream(instance.players.toArray(new GamePlayer[0])).forEach(gamePlayer -> {gamePlayer.getPlayer().sendTitle(title, subTitle, i, i1, i2);});
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

            if(this.blocks.get(player).contains(((BlockBreakEvent) event).getBlock()) && !containsController(GameController.BREAK_BLOCKS)){
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

        if(event instanceof PlayerPickupArrowEvent && ! containsController(GameController.ARROW_PICKUPS)){
            ((Cancellable) event).setCancelled(true);
        }

        if(event instanceof PlayerDeathEvent){
            PlayerDeathEvent deathEvent = (PlayerDeathEvent) event;

            if(!containsController(GameController.DEATH_DROPS)) {
                deathEvent.setDroppedExp(0);
                deathEvent.setKeepInventory(true);
                deathEvent.getEntity().getInventory().clear();

            }
        }

        if(event instanceof WeatherChangeEvent && !containsController(GameController.WEATHER)){
            ((Cancellable) event).setCancelled(true);
            return;
        }

        GamePlayer player;
        if(event instanceof PlayerChangedWorldEvent) {
            player = GamePlayer.getPlayer(((PlayerChangedWorldEvent) event).getPlayer());
        }else if(event instanceof PlayerQuitEvent){
            player = GamePlayer.getPlayer(((PlayerQuitEvent) event).getPlayer());
        }else{
            return;
        }

        if(players.contains(player) || spectators.contains(player)){
            players.remove(player);
            spectators.remove(player);
            player.getPlayer().sendMessage("§7You left the game with id: "+gameID);
        }
    }

    public void switchToSpectator(GamePlayer player){
        if(players.contains(player)){
            players.remove(player);
            spectators.add(player);
            player.getPlayer().setGameMode(GameMode.SPECTATOR);
        }
    }
}

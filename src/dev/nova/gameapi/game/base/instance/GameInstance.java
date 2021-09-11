package dev.nova.gameapi.game.base.instance;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.GameEvent;
import dev.nova.gameapi.game.base.scoreboard.Scoreboard;
import dev.nova.gameapi.game.base.values.GameValue;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.map.MapInjection;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.event.Event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class GameInstance {

    private final int gameID;

    private final GameMap gameMap;

    private final GameBase gameBase;

    private final ArrayList<GamePlayer> players;
    private final ArrayList<GameEvent> events;
    private final ArrayList<GameValue<?>> values;

    private final GameMap.Map map;
    private final GameUtils utils;
    private final String[] description;
    private final String brief;
    private final String displayName;

    private Scoreboard scoreboard;

    private GameState gameState;
    private boolean canStart = true;

    /**
     *
     * {@inheritDoc}
     * 
     * @param map The map the players are going to play in.
     */
    public GameInstance(String displayName,@Nonnull String gameBase, GameMap map,String[] gameDescription, String gameBrief) {
        this.gameBase = GameManager.getGame(gameBase);
        this.displayName = displayName;
        this.description =gameDescription;
        this.brief = gameBrief;
        this.gameID = this.gameBase.getRunningInstances().size() +1;
        this.gameMap = map;
        this.events = new ArrayList<>();
        this.players = new ArrayList<GamePlayer>();
        values = new ArrayList<>();

        this.map = map.newMap(this);

        this.gameState = GameState.CLOSED;

        this.utils = new GameUtils(this);

    }

    protected void setScoreboard(Scoreboard scoreboard){
        this.scoreboard = scoreboard;
        for(GamePlayer player : players){
            getScoreboard().addPlayer(player);
        }
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
        this.gameState = GameState.ENDED;
        if(gameMap != null){
            for(MapInjection injection : map.getCustomInjections()){
                injection.onEnd();
            }
        }
    }

    public void postEnd(){
        canStart = false;
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

    public void onEvent(Event event){}
}

package dev.nova.gameapi.game.queue;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.player.GamePlayer;

import java.util.ArrayList;
import java.util.Arrays;

public class Queue {

    public static final ArrayList<Queue> OPEN_QUEUES = new ArrayList<>();
    public static final ArrayList<Queue> CLOSED_QUEUES = new ArrayList<>();

    private final GameBase game;
    private final ArrayList<GamePlayer> playersQueued;
    private final GameMap map;
    private final int id;
    private final String instanceName;
    private final boolean privateQueue;
    private GameInstance instance;
    public boolean started;
    public boolean forceStart = false;
    private int coolDownTicks;

    public Queue(String instance, GameBase game, GameMap map,GamePlayer[] players) {
        this.game = game;
        if(players == null || players.length == 0){
            this.privateQueue = false;
        }else{
            this.privateQueue = true;
        }
        this.map = map;
        this.instanceName = instance;
        this.id = OPEN_QUEUES.size() + 1;
        this.playersQueued = new ArrayList<GamePlayer>();

        OPEN_QUEUES.add(this);
        if(privateQueue){
            for(GamePlayer player : players){
                addPlayer(player);
            }
        }
    }

    public Queue(String instance, GameBase game, GameMap map) {
        this(instance,game,map,new GamePlayer[0]);
    }

    public static Queue[] getQueues(GameBase game) {
        ArrayList<Queue> queues = new ArrayList<>();
        for (Queue queue : OPEN_QUEUES) {
            if (!CLOSED_QUEUES.contains(queue)) {
                if (queue.game.equals(game)) queues.add(queue);
            }
        }
        return queues.toArray(new Queue[0]);
    }

    public static Queue[] getQueues(int freeSlotsNeeded) {
        ArrayList<Queue> queues = new ArrayList<>();
        for (Queue queue : OPEN_QUEUES) {
            if (!CLOSED_QUEUES.contains(queue)) {
                if (queue.playersQueued.size() + freeSlotsNeeded <= queue.map.getPlayerLimit()) queues.add(queue);
            }
        }
        return queues.toArray(new Queue[0]);
    }

    public static Queue[] getQueues(GameBase game, int freeSlotsNeeded) {
        ArrayList<Queue> queues = new ArrayList<>();
        for (Queue queue : OPEN_QUEUES) {
            if (!CLOSED_QUEUES.contains(queue)) {
                if (queue.game.equals(game) && queue.playersQueued.size() + freeSlotsNeeded <= queue.map.getPlayerLimit())
                    queues.add(queue);

            }
        }
        return queues.toArray(new Queue[0]);
    }

    public static Queue getPlayerQueue(GamePlayer player) {
        Queue queueIn = null;

        for (Queue queue : OPEN_QUEUES) {
            if (queue.playersQueued.contains(player)) {
                queueIn = queue;
                break;
            }
        }

        for (Queue queue : CLOSED_QUEUES) {
            if (queueIn == null) {
                if (queue.playersQueued.contains(player)) {
                    queueIn = queue;
                    break;
                }
            } else {
                break;
            }
        }

        return queueIn;
    }

    public static boolean isPlayerInQueue(GamePlayer player) {
        return getPlayerQueue(player) != null;
    }

    public GameBase getGame() {
        return game;
    }

    public ArrayList<GamePlayer> getPlayersQueued() {
        return playersQueued;
    }

    public void addPlayer(GamePlayer player) {
        playersQueued.add(player);
        for(GamePlayer gamePlayer : playersQueued) {
            int needed = map.getPlayerLimit() - playersQueued.size();
            if (needed == 0) {
                gamePlayer.getPlayer().sendMessage("§8" + player.getPlayer().getName() + " §7has joined the queue! (Full queue!)");
            }else if(needed == 1){
                gamePlayer.getPlayer().sendMessage("§8" + player.getPlayer().getName() + " §7has joined the queue! (need "+needed+" more player)");
            }else{
                gamePlayer.getPlayer().sendMessage("§8" + player.getPlayer().getName() + " §7has joined the queue! (need "+needed+" more players)");
            }
        }
        if (playersQueued.size() == map.getPlayerLimit()) {
            onQueueFull();
        }
    }

    public void removePlayer(GamePlayer player) {
        playersQueued.remove(player);
        player.getPlayer().sendMessage("§cYou left the queue with id: §7" + id);
        if (playersQueued.size() < map.getPlayerLimit() - 1 && !forceStart) {
            onQueueUnFull();
        }
    }

    public void onQueueUnFull() {
        if(!forceStart) {
            if (instance == null) {
                if (!privateQueue) {
                    OPEN_QUEUES.add(this);
                    CLOSED_QUEUES.remove(this);
                }
                for (GamePlayer player : playersQueued) {
                    player.getPlayer().sendMessage("§cStarting cancelled because a player left the queue!");
                }
                this.started = false;
            } else {
                GameLogger.log(new GameLog(game, LogLevel.ERROR, "Queue with id: " + id + " invoked unfull after instance field been set!", true));
            }
        }
    }

    public GameMap getMap() {
        return map;
    }

    public void onQueueFull() {
            OPEN_QUEUES.remove(this);
            CLOSED_QUEUES.add(this);

        this.started = true;

        if (coolDownTicks == 0) {
            for (GamePlayer player : playersQueued) {
                player.getPlayer().sendMessage("§aGame is starting in: §75 seconds");
            }
        }



    }

    public int getId() {
        return id;
    }

    public void coolDownTick() {

        if (started) {
            coolDownTicks++;

            if (coolDownTicks == (5 * 20L) + 1) {
                if (instance == null) {
                    this.instance = game.newInstance(instanceName, map);
                    for (GamePlayer player : playersQueued) {
                        instance.join(player);
                    }
                    instance.onStart();
                } else {
                    GameLogger.log(new GameLog(game, LogLevel.ERROR, "The queue with id: " + id + " had an instance running but invoked onQueueFull", true));
                }
            }
        } else {
            coolDownTicks = 0;
        }
    }
}

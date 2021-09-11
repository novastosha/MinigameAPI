package dev.nova.gameapi.game.queue;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.player.GamePlayer;

import java.util.ArrayList;

public class Queue {

    public static final ArrayList<Queue> OPEN_QUEUES = new ArrayList<>();
    public static final ArrayList<Queue> CLOSED_QUEUES = new ArrayList<>();

    private final GameBase game;
    private final ArrayList<GamePlayer> playersQueued;
    private final GameMap map;
    private final int id;
    private GameInstance instance;
    private boolean started;
    private int coolDownTicks;

    public Queue(GameBase game, GameMap map){
        this.game = game;
        this.map = map;
        this.id = OPEN_QUEUES.size()+1;
        this.playersQueued = new ArrayList<GamePlayer>();
        OPEN_QUEUES.add(this);
    }

    public static Queue[] getQueues(GameBase game){
        ArrayList<Queue> queues = new ArrayList<>();
        for(Queue queue : OPEN_QUEUES){
            if (!CLOSED_QUEUES.contains(queue)) {
                if (queue.game.equals(game)) queues.add(queue);
            }
        }
        return queues.toArray(new Queue[0]);
    }

    public static Queue[] getQueues(int freeSlotsNeeded){
        ArrayList<Queue> queues = new ArrayList<>();
        for(Queue queue : OPEN_QUEUES){
            if (!CLOSED_QUEUES.contains(queue)) {
                if (queue.playersQueued.size() + freeSlotsNeeded <= queue.map.getPlayerLimit()) queues.add(queue);
            }
        }
        return queues.toArray(new Queue[0]);
    }

    public static Queue[] getQueues(GameBase game, int freeSlotsNeeded){
        ArrayList<Queue> queues = new ArrayList<>();
        for(Queue queue : OPEN_QUEUES) {
            if (!CLOSED_QUEUES.contains(queue)) {
                if (queue.game.equals(game) && queue.playersQueued.size() + freeSlotsNeeded <= queue.map.getPlayerLimit())
                    queues.add(queue);

            }
        }
        return queues.toArray(new Queue[0]);
    }

    public GameBase getGame() {
        return game;
    }

    public ArrayList<GamePlayer> getPlayersQueued() {
        return playersQueued;
    }

    public void addPlayer(GamePlayer player){
        playersQueued.add(player);
        if(playersQueued.size() == map.getPlayerLimit()){
            onQueueFull();
        }
    }

    public void removePlayer(GamePlayer player){
        playersQueued.remove(player);
        if(playersQueued.size() < map.getPlayerLimit()-1){
            onQueueUnFull();
        }
    }

    public void onQueueUnFull() {
        if(!OPEN_QUEUES.contains(this)){
            if(instance == null){
                OPEN_QUEUES.add(this);
                CLOSED_QUEUES.remove(this);
                for(GamePlayer player : playersQueued){
                    player.getPlayer().sendMessage("§cStarting cancelled because a player left the queue!");
                }
                this.started = false;
            }else{
                GameLogger.log(new GameLog(game,LogLevel.ERROR,"Queue with id: "+id+" invoked unfull after instance field been set!",true));
            }
        }
    }

    public GameMap getMap() {
        return map;
    }

    public void onQueueFull(){
        OPEN_QUEUES.remove(this);
        CLOSED_QUEUES.add(this);

        this.started = true;

        if(coolDownTicks == 0){
            for(GamePlayer player : playersQueued){
                player.getPlayer().sendMessage("§aGame is starting in: §710 seconds");
            }
        }



    }

    public int getId() {
        return id;
    }

    public void coolDownTick() {

        if(started) {
            coolDownTicks++;

            if(coolDownTicks == 1 * 20L) {
                if (instance == null) {
                    this.instance = game.newInstance(map);
                    for(GamePlayer player : playersQueued){
                        instance.join(player);
                    }
                    instance.onStart();
                } else {
                    GameLogger.log(new GameLog(game, LogLevel.ERROR, "The queue with id: " + id + " had an instance running but invoked onQueueFull", true));
                }
            }
        }else if(coolDownTicks != 0){
            coolDownTicks = 0;
        }
    }
}
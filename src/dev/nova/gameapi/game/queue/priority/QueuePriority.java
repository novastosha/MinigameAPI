package dev.nova.gameapi.game.queue.priority;

import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.game.queue.Queue;

public interface QueuePriority {

    Queue prioritize(GamePlayer[] player, Queue[] queues);

}

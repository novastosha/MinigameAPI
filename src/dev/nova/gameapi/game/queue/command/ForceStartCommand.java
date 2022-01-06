package dev.nova.gameapi.game.queue.command;

import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.game.queue.Queue;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForceStartCommand implements CommandExecutor {
    @Override
    public boolean onCommand( CommandSender commandSender,  Command command,  String s,  String[] strings) {
        if(!(commandSender instanceof Player playerB)){
            commandSender.sendMessage("§cOnly players can execute command!");
            return true;
        }

        GamePlayer player = GamePlayer.getPlayer(playerB);

        if(player.isInGame()){
            playerB.sendMessage("§cYou are already in a game!");
            return true;
        }

        if(!Queue.isPlayerInQueue(player)){
            playerB.sendMessage("§cYou are not in a queue!");
            return true;
        }


        Queue queue = Queue.getPlayerQueue(player);

        if(queue.started){
            playerB.sendMessage("§cThe queue has already started!");
            return true;
        }

        queue.forceStart = true;
        playerB.sendMessage("§cWARNING! This command may cause issues with your game!");
        queue.onQueueFull();

        return true;
    }
}

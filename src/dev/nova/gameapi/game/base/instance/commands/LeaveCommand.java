package dev.nova.gameapi.game.base.instance.commands;

import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.game.queue.Queue;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if(!(commandSender instanceof Player)){
            commandSender.sendMessage("§cOnly players can leave games!");
            return true;
        }

        GamePlayer player = GamePlayer.getPlayer(((Player) commandSender));

        if(!player.isInGame()){
            Queue queue = Queue.getPlayerQueue(player);
            if(queue != null){
                queue.removePlayer(player);
                return true;
            }
            player.getPlayer().sendMessage("§cYou are not in a game or a queue!");
            return true;
        }

        player.getGame().leave(player);

        return true;
    }

}

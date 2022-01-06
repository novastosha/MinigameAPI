package dev.nova.gameapi.game.base.instance.commands;

import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpectatorCommand implements CommandExecutor {
    @Override
    public boolean onCommand( CommandSender commandSender,  Command command,  String s,  String[] strings) {

        if(!(commandSender instanceof Player playerB)){
            commandSender.sendMessage("§cOnly players can execute this command!");
            return true;
        }

        GamePlayer player = GamePlayer.getPlayer(playerB);

        if(!player.isInGame()){
            playerB.sendMessage("§cYou are not in a game!");
            return true;
        }

        if(!player.getGame().getPlayers().contains(player)){
            playerB.sendMessage("§cYou are already a spectator!");
            return true;
        }

        player.getGame().switchToSpectator(player);
        playerB.sendMessage("§aYou are now a spectator");

        return true;
    }
}

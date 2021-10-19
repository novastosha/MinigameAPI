package dev.nova.gameapi.game.base.lobby.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if(!(commandSender instanceof Player player)){
            commandSender.sendMessage(ChatColor.RED+"Only players can execute lobby commands!");
            return true;
        }

        return true;
    }
}

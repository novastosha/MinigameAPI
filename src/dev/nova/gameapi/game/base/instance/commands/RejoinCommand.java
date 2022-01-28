package dev.nova.gameapi.game.base.instance.commands;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.GameState;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RejoinCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player Bplayer)){
            sender.sendMessage(ChatColor.RED+"Only players can execute this command!");
            return true;
        }

        GamePlayer player = GamePlayer.getPlayer(Bplayer);

        if(player.getPreviousGame() == null || player.getPreviousGame().getGameState().equals(GameState.CLOSED) || player.getPreviousGame().getGameState().equals(GameState.ENDED)){
            player.getPlayer().sendMessage(ChatColor.RED+"Your previous game has ended or you didn't join any game.");
            return true;
        }

        GameInstance instance = player.getPreviousGame();

        if(instance.getGameState().equals(GameState.NOT_STARTED)){
            player.getPlayer().sendMessage(ChatColor.RED+"Your previous game has not started and thus you cannot rejoin it.");
            return true;
        }

        if(!instance.isRejoinCapable()){
            player.getPlayer().sendMessage(ChatColor.RED+"Unfortunately, your game cannot be rejoined for now.");
            return true;
        }

        if(instance.rejoin(player)){
            player.getPlayer().sendMessage(ChatColor.GREEN+"Rejoined!");
        }else {
            player.getPlayer().sendMessage(ChatColor.RED+"There was an error while attempting to rejoin, if the error persists please report it to an admin!");
        }

        return true;
    }
}

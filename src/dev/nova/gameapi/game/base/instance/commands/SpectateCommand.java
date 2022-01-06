package dev.nova.gameapi.game.base.instance.commands;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpectateCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {

        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("§cOnly players can spectate games / players");
            return true;
        }

        if (args.length == 0) {
            commandSender.sendMessage("§cMissing arguments!");
            return true;
        }

        Player player = (Player) commandSender;
        if(GamePlayer.getPlayer(player).isInGame()){
            player.sendMessage("§cYou can't spectate while in a game!");
            return true;
        }

        if(args.length == 1){
            if(Bukkit.getServer().getPlayer(args[0]) == null){
                player.sendMessage("§cUnable to find: "+args[0]);
                return true;
            }

            GamePlayer gamePlayer = GamePlayer.getPlayer(Bukkit.getServer().getPlayerExact(args[0]));

            if(!gamePlayer.isInGame()){
                player.sendMessage("§c"+args[0]+" is not in a game!");
                return true;
            }

            if(!gamePlayer.getGame().canBeSpectated()){
                player.sendMessage("§cThe game of: "+args[0]+" cannot be spectated!");
                return true;
            }

            gamePlayer.getGame().joinSpectator(GamePlayer.getPlayer(player));
        }else if(args.length == 2){
            player.sendMessage("§cMissing game ID!");
            return true;
        }else if(args.length == 3){
            GameBase base = GameManager.getGame(args[0]);
            if (base != null) {
                ArrayList<GameInstance> instances = base.getRunningInstances().get(args[1]) != null ? base.getRunningInstances().get(args[1]) : new ArrayList<>();

                if (!instances.isEmpty()) {
                    for (GameInstance instance : instances) {
                        try {
                            if (instance.getGameID() == Integer.parseInt(args[2])) {
                                instance.joinSpectator(GamePlayer.getPlayer(player));
                                return true;
                            }
                        }catch (NumberFormatException e){
                            player.sendMessage("§c"+args[2]+" is not a game id (NaN)");
                            return true;
                        }
                    }
                }else{
                    player.sendMessage("§cNo games are running with that mode!");
                    return true;
                }
            }else{
                player.sendMessage("§cInvalid game codename!");
                return true;
            }
        }

        return true;
    }

    @Override
    public  List<String> onTabComplete( CommandSender commandSender,  Command command,  String s,  String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                GamePlayer gamePlayer = GamePlayer.getPlayer(player);

                if (gamePlayer.isInGame()) {
                    list.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            GameBase base = GameManager.getGame(args[0]);

            if (base != null) {
                list.addAll(base.getInstances().keySet());
            }
        } else if (args.length == 3) {
            GameBase base = GameManager.getGame(args[0]);
            if (base != null) {
                ArrayList<GameInstance> instances = base.getRunningInstances().get(args[1]) != null ? base.getRunningInstances().get(args[1]) : new ArrayList<>();

                if (!instances.isEmpty()) {
                    for (GameInstance instance : instances) {
                        list.add(String.valueOf(instance.getGameID()));
                    }
                }
            }
        }

        return list;
    }
}

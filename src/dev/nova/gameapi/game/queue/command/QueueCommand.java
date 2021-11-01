package dev.nova.gameapi.game.queue.command;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.game.queue.Queue;
import dev.nova.gameapi.party.Party;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class QueueCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {

        if(!(commandSender instanceof Player player)){
            commandSender.sendMessage("§cOnly players can execute this command!");
            return true;
        }

        GamePlayer gamePlayer = GamePlayer.getPlayer(player);
        if(gamePlayer.isInGame()){
            player.sendMessage("§cCannot queue while in a game!");
            return true;
        }

        if(args.length == 0){
            player.sendMessage("§cPlease type a game!");
            return true;
        }

        if(args.length == 1){
            player.sendMessage("§cPlease type an instance name!");
            return true;
        }

        if(args.length == 2){
            GameBase game = GameManager.getGame(args[0]);

            if(game == null){
                player.sendMessage("§cNo game exists with this name!");
                return true;
            }

            if(!game.instanceExists(args[1])){
                player.sendMessage("§cNo mode exists with name: "+args[1]);
                return true;
            }

            if(gamePlayer.isInParty()){
                if(gamePlayer != gamePlayer.getParty().getLeader()){
                    player.sendMessage("§cOnly the leader can queue you into games!");
                    return true;
                }
            }

            Queue queue;

            try{
                if(!gamePlayer.isInParty()) {
                    queue = Queue.getQueues(game, 1)[0];
                }else{
                    queue = Queue.getQueues(game, gamePlayer.getParty().getPartyMembers().size())[0];
                }
            }catch (ArrayIndexOutOfBoundsException e){
                queue = new Queue(args[1],game,game.random());
            }

            if(queue == null){
                queue = new Queue(args[1],game,game.random());
            }

            if(gamePlayer.isInParty()){
                if(queue.getMap().getPlayerLimit() < gamePlayer.getParty().getPartyMembers().size()){
                    Party.sendMessage(gamePlayer, Component.text("§cThe game that you are trying to queue in is too small!"));
                    return true;
                }
            }

            if(!gamePlayer.isInParty()) {
                player.sendMessage("§aYou have been queued in a queue with id: §7" + queue.getId());

                queue.addPlayer(GamePlayer.getPlayer(player));
            }else{
                gamePlayer.getParty().sendGlobalMessage(Component.text("§7The party leader is queuing you to: §3"+args[1]));
                for(GamePlayer player1 : gamePlayer.getParty().getPartyMembers().keySet()){
                    player1.getPlayer().sendMessage("§aYou have been queued in a queue with id: §7" + queue.getId());
                    queue.addPlayer(player1);
                }
            }
            return true;
        }else{
            player.sendMessage("§cToo many arguments!");
            return true;
        }
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<String> list = new ArrayList<>();

        if(args.length == 0 || args.length == 1){
            for(GameBase base : GameManager.GAMES){
                list.add(base.getCodeName().toLowerCase());
            }
            return list;
        }

        if(args.length == 2){
            GameBase base = GameManager.getGame(args[0]);

            if(base == null){
                return list;
            }

            list.addAll(base.getInstances().keySet());

            return list;
        }

        return list;
    }
}

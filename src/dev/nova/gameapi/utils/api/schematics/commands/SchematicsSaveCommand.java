package dev.nova.gameapi.utils.api.schematics.commands;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.utils.api.GameSaveData;
import dev.nova.gameapi.utils.api.general.ModifiablePair;
import dev.nova.gameapi.utils.api.schematics.Schematic;
import dev.nova.gameapi.utils.api.schematics.commands.listener.SchematicsInteractEventListener;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SchematicsSaveCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cPlease type a game!");
            return true;
        }

        if (args.length == 1) {
            player.sendMessage("§cPlease type an instance name!");
            return true;
        }



        if (args.length == 3) {
            GameBase game = GameManager.getGame(args[0]);

            if (game == null) {
                player.sendMessage("§cNo game exists with this name!");
                return true;
            }

            if (!game.instanceExists(args[1])) {
                player.sendMessage("§cNo mode exists with name: " + args[1]);
                return true;
            }

            String name = args[2];

            ModifiablePair<Location, Location> pair = SchematicsInteractEventListener.players.get(player);

            if(!SchematicsInteractEventListener.players.containsKey(player)){
                player.sendMessage("§cYou must set the locations!");
                return true;
            }

            if(pair.getA() == null || pair.getB() == null){
                player.sendMessage("§cYou must set the locations!");
                return true;
            }

            player.sendMessage("§7Saving schematic: "+name);
            Schematic schematic = Schematic.save(pair.getA(),pair.getB(),new GameSaveData(game,args[1]),name);
            Schematic.pasteSchematic(pair.getA().add(0,6,0),schematic);
            return true;
        }

        return true;
    }

}

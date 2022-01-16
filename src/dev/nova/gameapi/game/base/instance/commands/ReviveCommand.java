package dev.nova.gameapi.game.base.instance.commands;

import dev.nova.gameapi.GAPIPlugin;
import dev.nova.gameapi.utils.api.gui.ObjectBoundGUI;
import dev.nova.gameapi.utils.api.gui.item.ClickAction;
import dev.nova.gameapi.utils.api.gui.item.Item;
import dev.nova.gameapi.game.base.instance.team.Team;
import dev.nova.gameapi.game.base.instance.team.TeamGameInstance;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ReviveCommand implements CommandExecutor {
    @Override
    public boolean onCommand( CommandSender commandSender,  Command command,  String s,  String[] args) {

        if(!(commandSender instanceof Player playerB)){
            commandSender.sendMessage("§cOnly players can execute this command!");
            return true;
        }

        GamePlayer player = GamePlayer.getPlayer(playerB);

        if(!player.isInGame()){
            playerB.sendMessage("§cYou are not in a game!");
            return true;
        }

        if(player.getGame().getPlayers().contains(player)){
            playerB.sendMessage("§cYou are already alive!");
            return true;
        }

        if(player.getGame() instanceof TeamGameInstance){
            ObjectBoundGUI teamsGui = new ObjectBoundGUI("Which team do you want to be in?",6, GAPIPlugin.getPlugin(GAPIPlugin.class)) {};

            int i = 0;
            for(Team team : ((TeamGameInstance) player.getGame()).getTeams()){
                ItemStack teamItemStack = new ItemStack(Material.TERRACOTTA,1,(byte) team.getColor().ordinal());
                ItemMeta meta = teamItemStack.getItemMeta();

                ArrayList<String> lore = new ArrayList<>(List.of(new String[]{
                        " ",
                        "§7Click to join this team!",
                        "  ",
                        "§7Team members: "
                }));

                for(GamePlayer teamP : team.getPlayers()){
                    lore.add(" §8- "+team.getColor()+teamP.getPlayer().getName());
                }
                meta.setLore(lore);
                meta.setDisplayName(team.getColor()+"Team: "+team.getName().toUpperCase());

                teamItemStack.setItemMeta(meta);

                Item<Team> teamItem = new Item<>(i,teamItemStack,team);

                teamItem.setClickAction(new ClickAction() {
                    @Override
                    public void onClick(InventoryClickEvent event) {
                        player.getGame().getPlayers().add(player);
                        player.getGame().getSpectators().remove(player);
                        team.addPlayer(player);
                        player.getPlayer().teleport(team.getPlayers().get(0).getPlayer());
                        playerB.sendMessage("§aYou have been revived!");

                    }
                });

                teamsGui.addItem(teamItem);
                i++;
            }

            teamsGui.open(playerB);
        }else{
            player.getGame().getPlayers().add(player);
            player.getGame().getSpectators().remove(player);
            playerB.sendMessage("§aYou have been revived!");

        }


        return true;
    }
}

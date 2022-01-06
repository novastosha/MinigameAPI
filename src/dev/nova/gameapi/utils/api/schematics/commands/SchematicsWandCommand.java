package dev.nova.gameapi.utils.api.schematics.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SchematicsWandCommand implements CommandExecutor {

    public static final ItemStack wandItem;

    static {

        wandItem = new ItemStack(Material.STICK);

        ItemMeta meta = wandItem.getItemMeta();
        meta.setDisplayName("§7Selection wand");

        wandItem.setItemMeta(meta);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can execute this command!");
            return true;
        }

        if(player.getInventory().firstEmpty() != -1 && !player.getInventory().contains(wandItem)){
            player.getInventory().setItem(player.getInventory().firstEmpty(),wandItem);
            player.sendMessage("§7You received the selection wand");
            return true;
        }

        player.sendMessage("§cYour inventory is full or you have it already!");

        return true;
    }
}

package dev.nova.gameapi.utils.api.schematics.commands.listener;

import dev.nova.gameapi.utils.api.general.ModifiablePair;
import dev.nova.gameapi.utils.api.schematics.commands.SchematicsWandCommand;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;

public class SchematicsInteractEventListener implements Listener {

    public static HashMap<Player, ModifiablePair<Location,Location>> players = new HashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        ModifiablePair<Location,Location> pair = players.getOrDefault(e.getPlayer(),new ModifiablePair<>());

        if(e.getAction().equals(Action.LEFT_CLICK_BLOCK) && e.getPlayer().getItemInHand().getType() == SchematicsWandCommand.wandItem.getType() && e.getPlayer().getItemInHand().getItemMeta().getDisplayName().equals(SchematicsWandCommand.wandItem.getItemMeta().getDisplayName())){

            pair.setA(e.getClickedBlock().getLocation());
            players.put(e.getPlayer(),pair);
            e.setCancelled(true);
            e.getPlayer().sendMessage("§7Position 1 has been set!");


        }else if(e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && e.getPlayer().getItemInHand().getType() == SchematicsWandCommand.wandItem.getType() && e.getPlayer().getItemInHand().getItemMeta().getDisplayName().equals(SchematicsWandCommand.wandItem.getItemMeta().getDisplayName())){

            pair.setB(e.getClickedBlock().getLocation());
            players.put(e.getPlayer(),pair);
            e.setCancelled(true);
            e.getPlayer().sendMessage("§7Position 2 has been set!");

        }
    }
}

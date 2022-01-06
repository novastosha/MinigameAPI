package dev.nova.gameapi.game.base.instance.commands.gui.api.item;

import org.bukkit.event.inventory.InventoryClickEvent;

public interface ClickAction {

    /**
     *
     * Gets called when an item is clicked on the inventory.
     *
     * @param event The click event.
     */
    void onClick(InventoryClickEvent event);

}
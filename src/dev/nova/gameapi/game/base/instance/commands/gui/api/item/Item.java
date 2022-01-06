package dev.nova.gameapi.game.base.instance.commands.gui.api.item;

import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public class Item<T> {

    private final ItemStack item;
    private final int slot;
    private final T value;
    private ClickAction action;

    /**
     *
     * Creates the object bound item to add it to the inventory.
     *
     * @param slot The slot of the item.
     * @param item The item to display.
     * @param value The object its bound to.
     */
    public Item(int slot,ItemStack item,T value){
        this.item = item;
        this.slot = slot;
        this.value = value;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getSlot() {
        return slot;
    }

    public Item<T> setClickAction(ClickAction action){
        this.action = action;
        return this;
    }

    @Nullable
    public ClickAction getAction() {
        return action;
    }

    public T getValue() {
        return value;
    }
}

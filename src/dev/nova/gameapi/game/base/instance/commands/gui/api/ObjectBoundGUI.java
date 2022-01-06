package dev.nova.gameapi.game.base.instance.commands.gui.api;

import dev.nova.gameapi.game.base.instance.commands.gui.api.item.Item;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public abstract class ObjectBoundGUI implements Listener {

    private final ArrayList<Item<?>> items;

    private final String title;
    private final int size;

    private final Inventory inventory;

    /**
     *
     * Creates a special inventory that has items that are bound to Java Objects.
     *
     * @param title The title of the inventory.
     * @param size The size of the inventory (will be multiplied by 9)
     * @param plugin The plugin creating the GUI (To add the click listener)
     */
    public ObjectBoundGUI(String title, int size, JavaPlugin plugin){
        this.title = title;
        this.size = size*9;
        items = new ArrayList<>();
        this.inventory = createBukkitInventory();
        Bukkit.getServer().getPluginManager().registerEvents(this,plugin);

    }

    private Inventory createBukkitInventory() {

        return Bukkit.createInventory(null,size,title);
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public void addItem(Item<?> item){
        items.add(item);
        inventory.setItem(item.getSlot(), item.getItem());
    }

    @EventHandler
    public void onEvent(InventoryClickEvent event){
        Inventory clicked = event.getClickedInventory();
        if(clicked == null) return;

        if(clicked.equals(inventory)){
            event.setCancelled(true);
            Item<?> item = get(event.getSlot());
            if(item != null && item.getAction() != null){
                item.getAction().onClick(event);
            }

        }
    }

    @EventHandler
    public void onCloseE(InventoryCloseEvent event ){
        if(inventory == null) return;
            if (event.getInventory().equals(inventory)) {
                onClose(event);
            }
        }

    protected void onClose(InventoryCloseEvent event) {
    }

    private Item<?> get(int slot) {
        for(Item<?> item : items){
            if(item.getSlot() == slot) return item;
        }
        return null;
    }

    public void open(Player player){
        if(inventory == null) return;

        player.openInventory(inventory);
    }
}

package dev.nova.gameapi.utils.api.general;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {



    private final Material material;
    private ArrayList<Component> lore = new ArrayList<>();
    private SkullData skullData;
    private Integer durability;
    private ArrayList<ItemFlag> flags;
    private int amount = 1;

    private ItemBuilder(final Material material) {
        this.material = material;
    }

    public ItemBuilder setSkullData(SkullData data) {
        if(material == Material.PLAYER_HEAD){
            this.skullData = data;
        }
        return this;
    }

    public ItemBuilder setDurability(int durability) {
        this.durability = durability;
        return this;
    }

    public ItemBuilder setFlags(ItemFlag flag, ItemFlag... itemFlags) {
        ArrayList<ItemFlag> flags = new ArrayList<>();

        if(flag != null) {
            flags.add(flag);
        }

        if(itemFlags != null){
            flags.addAll(List.of(itemFlags));
        }

        this.flags = flags;
        return this;
    }

    public ItemBuilder setAmount(int amount){
        this.amount = amount;
        return this;
    }

    public ItemBuilder setLore(Component component,Component... components) {

        ArrayList<Component> components1 = new ArrayList<>();

        if(component != null) {
            components1.add(component);
        }

        if(components != null){
            components1.addAll(List.of(components));
        }
        
        this.lore = components1;

        return this;
    }

    public ItemStack toItemStack() {
        ItemStack itemStack = new ItemStack(material);

        itemStack.lore(lore);

        return itemStack;
    }

    public static ItemBuilder newBuilder(Material material) {
        if(material == null) material = Material.STONE;
        return new ItemBuilder(material);
    }

    public static class SkullData {

        private final String texture;
        private String owner;

        private SkullData(String texture) {
            this.owner = "none";
            this.texture = texture;
        }

        public String getTexture() {
            return texture;
        }

        public String getOwner() {
            return owner;
        }

        public SkullData setOwner(String name) {
            if(name == null) return this;
            this.owner = name;
            return this;
        }

        public static SkullData fromBase64(String b64){
            if(b64 == null) return null;
            return new SkullData(b64);
        }

        public static SkullData fromProfile(PlayerProfile profile) {
            if(profile == null) return null;
            if(!profile.hasProperty("textures")) return null;

            SkullData data = new SkullData(profile.getProperties().stream().filter(profileProperty -> !profileProperty.getName().equalsIgnoreCase("textures")).findFirst().get().getValue());

            if(profile.getName() != null) {
                data.setOwner(profile.getName());
            }

            return data;
        }

    }
}

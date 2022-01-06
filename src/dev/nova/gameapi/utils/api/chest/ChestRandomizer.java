package dev.nova.gameapi.utils.api.chest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ChestRandomizer {

    private final Map<Integer, ItemStack> contents;

    private Map<Integer, ItemStack> parseData() {
        ConfigurationSection contents = dataConfig.getConfigurationSection("data");

        Map<Integer,ItemStack> items = new HashMap<>();
        if(contents == null) return items;
        for(String key : contents.getKeys(false)){
            Map<Enchantment, Integer> enchantments = new HashMap<>();


            int slot = 0;
            try{
                slot = Integer.parseInt(key);
            }catch (NumberFormatException e){
                continue;
            }

            int amount = 1;
            Material material = Material.AIR;

            ConfigurationSection slotConfig = contents.getConfigurationSection(key);

            if(slotConfig == null) continue;

            try{
                material = Material.valueOf(slotConfig.getString("material").toUpperCase());
            }catch (IllegalArgumentException e){
                e.printStackTrace();
                continue;
            }

            amount = slotConfig.getInt("amount");
            if(amount == 0){
                amount = 1;
            }

            ConfigurationSection enchants = slotConfig.getConfigurationSection("enchantments");

            if(enchants != null){
                for(String enchKey : enchants.getKeys(false)) {
                    ConfigurationSection enchantment = enchants.getConfigurationSection(enchKey);

                    if (enchantment == null) {
                        continue;
                    }

                    Enchantment enchant;

                    try {
                        enchant = Enchantment.getByName(enchantment.getString("enchantment-name").toUpperCase());
                    }catch (IllegalArgumentException e){
                        continue;
                    }

                    int level = enchantment.getInt("level");
                    if(level == 0){
                        continue;
                    }

                    enchantments.put(enchant,level);
                }
            }

            ItemStack itemStack = new ItemStack(material,amount);

            if(material.equals(Material.POTION)){
                PotionMeta potion = (PotionMeta) itemStack.getItemMeta();

                boolean upgraded = false;
                boolean extended = false;

                ConfigurationSection potionConfig = slotConfig.getConfigurationSection("potion-config");

                if(potionConfig == null) continue;

                if(potionConfig.contains("upgraded")){
                    upgraded = potionConfig.getBoolean("upgraded");
                }

                if(potionConfig.contains("extended")){
                    extended = potionConfig.getBoolean("extended");
                }

                try {
                    PotionType type = PotionType.valueOf(potionConfig.getString("type").toUpperCase());
                    potion.setBasePotionData(new PotionData(type,extended,upgraded));
                }catch (IllegalArgumentException e){
                    continue;
                }

                itemStack.setItemMeta(potion);
            }

            itemStack.addUnsafeEnchantments(enchantments);

            items.put(slot,itemStack);

        }
        return items;
    }

    private static final Random random = new Random();

    private final YamlConfiguration dataConfig;

    public ChestRandomizer(File file) {

        YamlConfiguration data = new YamlConfiguration();

        try {
            data.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        this.dataConfig = data;
        this.contents = parseData();
    }

    public YamlConfiguration getData() {
        return dataConfig;
    }

    public void randomize(Location[] blocks, boolean clearChests){
        for(Location location : blocks){

            if(location == null) continue;

            Block block = location.getBlock();

            if(block.getType().equals(Material.CHEST)){
                Chest chest = (Chest) block.getState();

                Inventory inventory = chest.getBlockInventory();
                if(clearChests) inventory.clear();

                for (int i = 0; i < ThreadLocalRandom.current().nextInt(9, 27 + 1); i++) {
                    int rnd = !(contents.size() <= 0) ? random.nextInt(contents.size()) : 0;

                    if(contents.containsKey(rnd) && !inventory.contains(contents.get(rnd).getType())){
                        inventory.setItem(random.nextInt(27),contents.get(rnd));
                    }
                }
            }
        }
    }


}

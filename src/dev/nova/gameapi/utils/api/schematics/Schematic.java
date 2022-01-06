package dev.nova.gameapi.utils.api.schematics;

import dev.nova.gameapi.utils.Files;
import dev.nova.gameapi.utils.api.GameSaveData;
import dev.nova.gameapi.utils.api.general.Pages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundGroup;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Schematic {

    private final YamlConfiguration dataConfig;
    private final HashMap<String, BlockDataHolder> locations;

    public static Schematic save(Location location, Location location1, GameSaveData data, String name) throws IllegalStateException {
        if (location.getWorld() != location1.getWorld()) {
            throw new IllegalStateException("Locations must be on the same world!");
        }

        if (location.getBlockY() < 0 || location1.getBlockY() < 0) {
            throw new IllegalStateException("Locations cannot have a Y coordinate that is less than 0!");
        }

        File folder = Files.getGameSchematicsFolder(data.base().getCodeName(), data.instance());
        folder.mkdirs();

        YamlConfiguration configuration = new YamlConfiguration();

        File file = new File(folder, name.endsWith(".yml") ? name : name + ".yml");

        Cuboid cuboid = new Cuboid(location, location1);

        try {
            file.createNewFile();
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            return null;
        }

        List<Block> blocks = cuboid.getBlocks();

        Location workingAt = location.getBlockY() > location1.getBlockY() ? location : location1;

        for (Block block : blocks) {
            if (block.getType().equals(Material.AIR)) continue;

            int relativeX = 0, relativeY = 0, relativeZ = 0;


            if (workingAt.getBlockX() >= block.getX()) {
                relativeX = workingAt.getBlockX() - block.getX();
            } else if (workingAt.getBlockX() < block.getX()) {
                relativeX = block.getX() - workingAt.getBlockX();
            }

            if (workingAt.getBlockY() >= block.getY()) {
                relativeY = workingAt.getBlockY() - block.getY();
            } else if (workingAt.getBlockY() < block.getY()) {
                relativeY = block.getY() - workingAt.getBlockY();
            }

            if (workingAt.getBlockZ() >= block.getZ()) {
                relativeZ = workingAt.getBlockZ() - block.getZ();
            } else if (workingAt.getBlockZ() < block.getZ()) {
                relativeZ = block.getZ() - workingAt.getBlockZ();
            }

            String toSave = "";

            switch (checkState(relativeX)) {
                case POSITIVE:
                    toSave = toSave + "0+" + relativeX;
                    break;
                case NEGATIVE:
                    toSave = toSave + "0-" + relativeX;
                    break;
                case ZERO:
                    toSave = toSave + "0";
                    break;
            }

            switch (checkState(relativeY)) {
                case POSITIVE:
                    toSave = toSave + "_0+" + relativeY;
                    break;
                case NEGATIVE:
                    toSave = toSave + "_0-" + relativeY;
                    break;
                case ZERO:
                    toSave = toSave + "_0";
                    break;
            }

            switch (checkState(relativeZ)) {
                case POSITIVE:
                    toSave = toSave + "_0+" + relativeZ;
                    break;
                case NEGATIVE:
                    toSave = toSave + "_0-" + relativeZ;
                    break;
                case ZERO:
                    toSave = toSave + "_0";
                    break;
            }

            configuration.set("schematic.blocks." + toSave + ".type", block.getType().name());
            configuration.set("schematic.blocks." + toSave + ".data", block.getState().getBlockData().getAsString());
            try {
                configuration.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return new Schematic(file);
    }

    private Schematic(File file) {

        YamlConfiguration data = new YamlConfiguration();

        try {
            data.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        this.dataConfig = data;
        this.locations = new HashMap<>();

        YamlConfiguration configuration = new YamlConfiguration();

        try {
            configuration.load(file);
        } catch (InvalidConfigurationException | IOException var3) {
            var3.printStackTrace();

        }

        configuration.getConfigurationSection("schematic.blocks").getKeys(false).forEach((blockLocation) -> {
            locations.put(blockLocation, new BlockDataHolder(Material.valueOf(configuration.getConfigurationSection("schematic.blocks").getString(blockLocation + ".type")), configuration.getConfigurationSection("schematic.blocks").getString(blockLocation + ".data")));
        });

    }

    public HashMap<String, BlockDataHolder> getLocations() {
        return locations;
    }


    public static NumberState checkState(int n) {
        if (n > 0) {
            return NumberState.POSITIVE;
        } else {
            return n < 0 ? NumberState.NEGATIVE : NumberState.ZERO;
        }
    }

    public static void pasteSchematic(Location center, Schematic schematic) {

        center = center.toCenterLocation();

        List<String> locations = new ArrayList<>(schematic.getLocations().keySet());
        List<BlockDataHolder> values = new ArrayList<>(schematic.getLocations().values());

        Collections.reverse(locations);
        Collections.reverse(values);

        int index = 0;
        for (String location : locations) {
            BlockDataHolder dataHolder = values.get(index);

            String[] xyz = location.split("_");
            int pasteX = 0;
            int pasteY = 0;
            int pasteZ = 0;
            int inCoordinate = 0;

            for (String coordinate : xyz) {
                if (coordinate.contains("+")) {
                    switch (inCoordinate) {
                        case 0:
                            pasteX = Integer.parseInt(coordinate.split("\\+")[1]);
                            break;
                        case 1:
                            pasteY = Integer.parseInt(coordinate.split("\\+")[1]);
                            break;
                        case 2:
                            pasteZ = Integer.parseInt(coordinate.split("\\+")[1]);
                    }
                } else if (coordinate.contains("-")) {
                    switch (inCoordinate) {
                        case 0:
                            pasteX = -Integer.parseInt(coordinate.split("-")[1]);
                            break;
                        case 1:
                            pasteY = -Integer.parseInt(coordinate.split("-")[1]);
                            break;
                        case 2:
                            pasteZ = -Integer.parseInt(coordinate.split("-")[1]);
                    }
                }

                inCoordinate++;
            }

            Location pasteLocation = new Location(center.getWorld(), (center.getBlockX() + pasteX), (center.getBlockY() + pasteY), (center.getBlockZ() + pasteZ));
            pasteLocation.getWorld().getBlockAt(pasteLocation).setType(values.get(index).material());
            pasteLocation.getWorld().getBlockAt(pasteLocation).getState().setBlockData(Bukkit.createBlockData(values.get(index).data()));
            center = new Location(center.getWorld(), center.getBlockX(), center.getBlockY(), center.getBlockZ());

            index++;
        }
    }

    private static enum NumberState {
        POSITIVE,
        NEGATIVE,
        ZERO
    }

}

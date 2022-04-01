package dev.nova.gameapi.game.map;

import dev.nova.gameapi.game.base.GameBase;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;

public interface Map {

    GameBase getGameBase();

    String getCodeName();

    String getDisplayName();

    ConfigurationSection getOptionsSection();

    void updateOptions(ConfigurationSection temporaryConfig);

    File getFile();

    ConfigurationSection getRawData();

}

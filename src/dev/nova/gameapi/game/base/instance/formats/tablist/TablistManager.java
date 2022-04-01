package dev.nova.gameapi.game.base.instance.formats.tablist;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.utils.Titles;
import org.bukkit.Bukkit;

public class TablistManager {

    private static final String serverVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    public static void setHeaderAndFooter(GameInstance instance) {

        if (!instance.isTablistFormatNull()) {
            for (GamePlayer player : instance.getPlayers()) {
                try {
                    Titles.sendTabList(instance.getTablistHeaderFormat().apply(player),instance.getTablistLowerFormat().apply(player),player.getPlayer());
                }catch (Exception ignored) {

                }
            }
        }
    }

    public static void resetTablist(GamePlayer player) {
        Titles.sendTabList("","", player.getPlayer());
    }
}

package dev.nova.gameapi.game.base.instance.formats.tablist;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.player.GamePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;

public class TablistManager {

    private static final String serverVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    public static void setHeaderAndFooter(GameInstance instance) {

        if (!instance.isTablistFormatNull()) {
            for (GamePlayer player : instance.getPlayers()) {

                setTablist(player,Component.text(instance.getTablistHeaderFormat().apply(player)),Component.text(instance.getTablistLowerFormat().apply(player)));
            }
        }
    }

    private static void setTablist(GamePlayer player,Component header, Component footer) {
        try {



            Class<?> tablistPacketClass;
            Object tablistPacket;
            Class<?> rawPacketClass;
            if (!serverVersion.startsWith("v1_17")) {
                tablistPacketClass = Class.forName("net.minecraft.server." + serverVersion + ".PacketPlayOutPlayerListHeaderFooter");
                rawPacketClass = Class.forName("net.minecraft.server." + serverVersion + ".Packet");
                tablistPacket = tablistPacketClass.getDeclaredConstructor().newInstance();
            } else {
                Class<?> comp = Class.forName("net.minecraft.network.chat.IChatBaseComponent");
                tablistPacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutPlayerListHeaderFooter");
                tablistPacket = tablistPacketClass.getConstructor(comp, comp).newInstance(null, null);
                rawPacketClass = Class.forName("net.minecraft.network.protocol.Packet");
            }


            tablistPacket.getClass().getField("adventure$header").set(tablistPacket, header);
            tablistPacket.getClass().getField("adventure$footer").set(tablistPacket, footer);

            Class<?> rawCraftPlayer = Class.forName("org.bukkit.craftbukkit." + serverVersion + ".entity.CraftPlayer");

            Object entityPlayer = rawCraftPlayer.getDeclaredMethod("getHandle").invoke(player.getPlayer());


            Object connection;
            if (!serverVersion.startsWith("v1_17")) {
                connection = entityPlayer.getClass().getDeclaredField("playerConnection").get(entityPlayer);
            } else {
                connection = entityPlayer.getClass().getDeclaredField("b").get(entityPlayer);
            }


            connection.getClass().getMethod("sendPacket", rawPacketClass).invoke(connection, tablistPacket);


        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void resetTablist(GamePlayer player) {
        setTablist(player,Component.text(" "),Component.text(" "));
    }

}

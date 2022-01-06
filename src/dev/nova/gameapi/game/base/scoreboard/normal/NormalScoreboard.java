package dev.nova.gameapi.game.base.scoreboard.normal;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import dev.nova.gameapi.game.base.scoreboard.Scoreboard;
import dev.nova.gameapi.game.base.scoreboard.ScoreboardLine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class NormalScoreboard implements Scoreboard {

    public static final String serverVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    protected Function<Player,String> title;
    private final HashSet<ScoreboardLine> lines;
    private final ArrayList<Player> players;

    public NormalScoreboard(Function<Player,String> title) {
        this.title = title;
        this.lines = new HashSet<ScoreboardLine>();
        this.players = new ArrayList<>();
    }

    @Override
    public void addPlayer(Player player) {
        this.players.add(player);
    }

    @Override
    public void removePlayer(Player player) {
        this.players.remove(player);
    }

    @Override
    public Function<Player,String> getTitle() {
        return title;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateScoreboard(Player player) {
        try {


            Class<?> rawScoreboardClass;
            Class<?> chatComponent;
            Class<?> scoreboardCriteria;
            Class<?> objectiveClass;
            Class<?> objectivePacketClass;
            Class<?> objectiveDisplayPacketClass;
            Class<?> scoreAddPacketClass;
            Class<?> baseComponentClass;
            Class<Enum> scoreboardCriteriaHealth;
            Class<Enum> actionEnum;
            Class<?> scoreClass;


            if (!serverVersion.startsWith("v1_17")) {
                scoreClass = Class.forName("net.minecraft.server." + serverVersion + ".ScoreboardScore");
                scoreAddPacketClass = Class.forName("net.minecraft.server." + serverVersion + ".PacketPlayOutScoreboardScore");
                rawScoreboardClass = Class.forName("net.minecraft.server." + serverVersion + ".Scoreboard");
                objectiveClass = Class.forName("net.minecraft.server." + serverVersion + ".ScoreboardObjective");
                chatComponent = Class.forName("net.minecraft.server." + serverVersion + ".ChatComponentText");
                baseComponentClass = Class.forName("net.minecraft.server." + serverVersion + ".IChatBaseComponent");
                scoreboardCriteria = Class.forName("net.minecraft.server." + serverVersion + ".IScoreboardCriteria");
                objectivePacketClass = Class.forName("net.minecraft.server." + serverVersion + ".PacketPlayOutScoreboardObjective");
                objectiveDisplayPacketClass = Class.forName("net.minecraft.server." + serverVersion + ".PacketPlayOutScoreboardDisplayObjective");
                scoreboardCriteriaHealth = (Class<Enum>) Class.forName("net.minecraft.server." + serverVersion + ".IScoreboardCriteria$EnumScoreboardHealthDisplay");
                actionEnum = (Class<Enum>) Class.forName("net.minecraft.server." + serverVersion + ".ScoreboardServer$Action");

            } else {
                scoreClass = Class.forName("net.minecraft.world.scores.ScoreboardScore");
                scoreAddPacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardScore");
                objectiveClass = Class.forName("net.minecraft.world.scores.ScoreboardObjective");
                rawScoreboardClass = Class.forName("net.minecraft.world.scores.Scoreboard");
                chatComponent = Class.forName("net.minecraft.network.chat.ChatComponentText");
                baseComponentClass = Class.forName("net.minecraft.network.chat.IChatBaseComponent");
                scoreboardCriteria = Class.forName("net.minecraft.world.scores.criteria.IScoreboardCriteria");
                objectiveDisplayPacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective");

                objectivePacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective");
                scoreboardCriteriaHealth = (Class<Enum>) Class.forName("net.minecraft.world.scores.criteria.IScoreboardCriteria$EnumScoreboardHealthDisplay");
                actionEnum = (Class<Enum>) Class.forName("net.minecraft.server.ScoreboardServer$Action");
            }



            Object nmsScoreboard = rawScoreboardClass.getDeclaredConstructor().newInstance();

            Object objective = nmsScoreboard.getClass().getMethod("registerObjective", String.class, scoreboardCriteria, baseComponentClass, scoreboardCriteriaHealth)
                    .invoke(nmsScoreboard, "scoreboard", !serverVersion.startsWith("v1_17") ? scoreboardCriteria.getField("DUMMY").get(null) : scoreboardCriteria.getField("a").get(null), chatComponent.getConstructor(String.class).newInstance(title.apply(player))
                            , scoreboardCriteriaHealth.getEnumConstants()[0]);

            nmsScoreboard.getClass().getMethod("setDisplaySlot", int.class, objectiveClass).invoke(nmsScoreboard, 1, objective);

            createScores(rawScoreboardClass, nmsScoreboard, objectiveClass, objective, player,this);


            Object createObjectivePacket = objectivePacketClass.getConstructor(objectiveClass, int.class).newInstance(objective, 0);
            Object deleteObjectivePacket = objectivePacketClass.getConstructor(objectiveClass, int.class).newInstance(objective, 1);
            Object displayPacket = objectiveDisplayPacketClass.getConstructor(int.class, objectiveClass).newInstance(1, objective);

            sendPacket(player, deleteObjectivePacket);
            sendPacket(player, createObjectivePacket);

            synchronized (this) {
                for (ScoreboardLine line : lines) {
                    Object addPacket = scoreAddPacketClass.getConstructor(actionEnum, String.class, String.class, int.class).newInstance(actionEnum.getEnumConstants()[0], "scoreboard", line.getContent().apply(player), line.getIndex());
                    sendPacket(player, addPacket);
                }

                sendPacket(player, displayPacket);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void updateTitle(Player player) {

    }


    public static void createScores(Class<?> scoreboardClass,Object board,Class<?> objectiveClass,Object objective,Player player,Scoreboard scoreboard) {
        try {
            Class<?> scoreClass;
            if (!serverVersion.startsWith("v1_17")) {
                scoreClass = Class.forName("net.minecraft.server." + serverVersion + ".ScoreboardScore");
            } else {
                scoreClass = Class.forName("net.minecraft.world.scores.ScoreboardScore");
            }

            for(ScoreboardLine line : scoreboard.getLines()){
                scoreClass.getConstructor(scoreboardClass,objectiveClass,String.class).newInstance(board,objective,line.getContent().apply(player));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void sendPacket(Player player, Object packet) {
        try {
            Class<?> rawCraftPlayer = Class.forName("org.bukkit.craftbukkit." + serverVersion + ".entity.CraftPlayer");

            Object entityPlayer = rawCraftPlayer.getDeclaredMethod("getHandle").invoke(player.getPlayer());


            Object connection;
            Class<?> rawPacketClass;
            if (!serverVersion.startsWith("v1_17")) {
                connection = entityPlayer.getClass().getDeclaredField("playerConnection").get(entityPlayer);
                rawPacketClass = Class.forName("net.minecraft.server." + serverVersion + ".Packet");
            } else {
                connection = entityPlayer.getClass().getDeclaredField("b").get(entityPlayer);
                rawPacketClass = Class.forName("net.minecraft.network.protocol.Packet");
            }


            connection.getClass().getMethod("sendPacket", rawPacketClass).invoke(connection, packet);
        } catch (Exception e) {

        }
    }

    @Override
    public void setLines(ScoreboardLine[] lines) {
        this.lines.clear();
        this.lines.addAll(List.of(lines));
    }

    @Override
    public Set<ScoreboardLine> getLines() {
        return lines;
    }

    @Override
    public List<Player> getPlayers() {
        return players;
    }
}

package dev.nova.gameapi.game.base.scoreboard.animated;

import dev.nova.gameapi.GAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import dev.nova.gameapi.game.base.scoreboard.Scoreboard;
import dev.nova.gameapi.game.base.scoreboard.ScoreboardLine;
import dev.nova.gameapi.game.base.scoreboard.animated.type.AnimationType;
import dev.nova.gameapi.game.base.scoreboard.normal.NormalScoreboard;

import java.util.*;
import java.util.function.Function;

import static dev.nova.gameapi.game.base.scoreboard.normal.NormalScoreboard.sendPacket;
import static dev.nova.gameapi.game.base.scoreboard.normal.NormalScoreboard.serverVersion;

public class AnimatedScoreboard implements Scoreboard {

    private final HashSet<ScoreboardLine> lines;
    private final AnimationType animationType;
    private final HashMap<Player, Integer> inChar;
    private final HashMap<Player, Object> playerObjectives;
    private final Function<Player, String> title;
    private final ArrayList<Player> players;

    public AnimatedScoreboard(Function<Player, String> title, AnimationType animationType) {

        this.title = title;

        this.animationType = animationType;
        this.inChar = new HashMap<>();
        this.playerObjectives = new HashMap<>();
        this.players = new ArrayList<>();
        this.lines = new HashSet<>();

        if (animationType != null) {

            Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(GAPIPlugin.getPlugin(GAPIPlugin.class), new Runnable() {
                @Override
                public void run() {
                    for (Player player : getPlayers()) {
                        updateTitle(player);
                    }
                }
            },  ((AnimationType.COLOR_ROLL) animationType).getDelay(),((AnimationType.COLOR_ROLL) animationType).getDelay());
        }
    }

    private void updateTitle(Player player) {

        AnimationType.COLOR_ROLL color_roll = (AnimationType.COLOR_ROLL) animationType;

        if (inChar.get(player) == title.apply(player).length()) {
            inChar.put(player, 0);
        }


        String strOriginal = ChatColor.stripColor(title.apply(player));
        String done = "";

        int index = 0;
        for (char c : strOriginal.toCharArray()) {
            if (index < inChar.get(player)) {
                done = done + color_roll.getDoneCharacters() +"§l"+ c;
            } else if (index == inChar.get(player)) {
                done = done + color_roll.getCharacterIn() +"§l"+ c;
            } else {
                done = done + color_roll.getCharactersNotDone() +"§l"+ c;
            }
            index++;
        }


        inChar.put(player, inChar.get(player) + 1);


        try {
            Class<?> objectiveClass;
            Class<?> objectivePacketClass;
            Class<?> objectiveDisplayPacketClass;
            Class<?> chatComponent;
            Class<?> baseComponentClass;


            if (!serverVersion.startsWith("v1_17")) {
                objectiveClass = Class.forName("net.minecraft.server." + serverVersion + ".ScoreboardObjective");
                chatComponent = Class.forName("net.minecraft.server." + serverVersion + ".ChatComponentText");
                baseComponentClass = Class.forName("net.minecraft.server." + serverVersion + ".IChatBaseComponent");
                objectivePacketClass = Class.forName("net.minecraft.server." + serverVersion + ".PacketPlayOutScoreboardObjective");
                objectiveDisplayPacketClass = Class.forName("net.minecraft.server." + serverVersion + ".PacketPlayOutScoreboardDisplayObjective");

            } else {
                objectiveClass = Class.forName("net.minecraft.world.scores.ScoreboardObjective");
                chatComponent = Class.forName("net.minecraft.network.chat.ChatComponentText");
                baseComponentClass = Class.forName("net.minecraft.network.chat.IChatBaseComponent");
                objectiveDisplayPacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective");
                objectivePacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective");
            }

            Object objective = playerObjectives.get(player);
            objective.getClass().getMethod("setDisplayName",baseComponentClass).invoke(objective,chatComponent.getConstructor(String.class).newInstance(ChatColor.BOLD+done));

            Object createObjectivePacket = objectivePacketClass.getConstructor(objectiveClass, int.class).newInstance(objective, 0);
            Object deleteObjectivePacket = objectivePacketClass.getConstructor(objectiveClass, int.class).newInstance(objective, 1);
            Object displayPacket = objectiveDisplayPacketClass.getConstructor(int.class, objectiveClass).newInstance(1, objective);

            sendPacket(player, deleteObjectivePacket);
            sendPacket(player, createObjectivePacket);
            sendPacket(player, displayPacket);
        } catch (
                Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void addPlayer(Player player) {
        this.players.add(player);
        inChar.put(player, 0);
        updateScoreboard(player);
    }

    @Override
    public void removePlayer(Player player) {
        this.players.remove(player);
        inChar.remove(player);
    }

    @Override
    public Function<Player, String> getTitle() {
        return title;
    }

    @Override
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

            Object objR = nmsScoreboard.getClass().getMethod("registerObjective", String.class, scoreboardCriteria, baseComponentClass, scoreboardCriteriaHealth)
                    .invoke(nmsScoreboard, "scoreboard", !serverVersion.startsWith("v1_17") ? scoreboardCriteria.getField("DUMMY").get(null) : scoreboardCriteria.getField("a").get(null), chatComponent.getConstructor(String.class).newInstance(ChatColor.BOLD+title.apply(player))
                            , scoreboardCriteriaHealth.getEnumConstants()[0]);

            Object objective = playerObjectives.getOrDefault(player, objR);

            if (!playerObjectives.containsKey(player)) {
                playerObjectives.put(player, objR);
            }

            nmsScoreboard.getClass().getMethod("setDisplaySlot", int.class, objectiveClass).invoke(nmsScoreboard, 1, objective);

            NormalScoreboard.createScores(rawScoreboardClass, nmsScoreboard, objectiveClass, objective, player, this);


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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setLines(ScoreboardLine[] lines) {
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

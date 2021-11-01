package dev.nova.gameapi.game.base.scoreboard.player;

import dev.nova.gameapi.GAPIPlugin;
import dev.nova.gameapi.game.base.instance.events.scoreboard.EventLine;
import dev.nova.gameapi.game.base.scoreboard.Scoreboard;
import dev.nova.gameapi.game.base.scoreboard.ScoreboardLine;
import dev.nova.gameapi.game.player.GamePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class PlayerScoreboard implements Scoreboard {

    private final org.bukkit.scoreboard.Scoreboard bukkitscoreboard;
    public int eventTaskID;
    private String title;
    private final GamePlayer player;
    private ChatColor theme;
    private List<ScoreboardLine> lines;
    private Objective objective;
    private boolean eventLinePreset = false;

    public PlayerScoreboard(ChatColor theme, String title, GamePlayer player, ScoreboardLine... initLines){
        this.theme = theme;
        this.title = title;
        this.player = player;
        this.lines = new LinkedList<>();
        for(ScoreboardLine line : initLines){
            lines.add(line);
        }
        this.bukkitscoreboard = Bukkit.getServer().getScoreboardManager().getNewScoreboard();
    }

    @Override
    public ChatColor getTheme() {
        return theme;
    }

    @Override
    public void setTheme(ChatColor theme) {
        this.theme = theme;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public ScoreboardLine[] getScoreboardLines() {
        return lines.toArray(new ScoreboardLine[0]);
    }

    @Override
    public org.bukkit.scoreboard.Scoreboard getBukkitScoreboard() {
        return bukkitscoreboard;
    }


    //Not implemented
    @Override
    public void addPlayer(GamePlayer player) {

    }

    //Not implemented
    @Override
    public void removePlayer(GamePlayer player) {

    }

    @Override
    public void updateScoreboard() {
        if(player.getGame() != null) {
            if (lines == null) return;

            this.objective = getBukkitScoreboard().registerNewObjective(UUID.randomUUID().toString().split("-")[0], "dummy", Component.text("dummy"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.displayName(Component.text(theme + "§l" + title.toUpperCase()));

            for (ScoreboardLine line : lines) {
                objective.getScore(line.getAsString(this)).setScore(line.getIndex());
            }

            player.getPlayer().setScoreboard(getBukkitScoreboard());
        }
    }

    @Override
    public void setScoreboardLine(ScoreboardLine line) {
        lines.set(line.getIndex(),line);

        if(line instanceof EventLine && !eventLinePreset){
            eventLinePreset = true;
            this.eventTaskID = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(GAPIPlugin.getPlugin(GAPIPlugin.class), new Runnable() {
                @Override
                public void run() {
                    updateScoreboard();
                }
            }, 0L, 21L);
        }
    }

    @Override
    public void setScoreboardLines(ScoreboardLine[] lines) {
        this.lines.clear();
        this.lines.addAll(Arrays.asList(lines));

        createEventLineTask(lines);
    }

    private void createEventLineTask(ScoreboardLine[] lines) {
        if(!eventLinePreset) {
            for (ScoreboardLine scoreboardLine : lines){
                if(scoreboardLine instanceof EventLine){
                    eventLinePreset = true;
                    this.eventTaskID = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(GAPIPlugin.getPlugin(GAPIPlugin.class), new Runnable() {
                        @Override
                        public void run() {
                            updateScoreboard();
                        }
                    }, 0L, 21L);
                }
            }
        }
    }

    @Override
    public void addLine(ScoreboardLine line) {
        lines.add(line);

        if(line instanceof EventLine && !eventLinePreset){
            eventLinePreset = true;
            this.eventTaskID=Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(GAPIPlugin.getPlugin(GAPIPlugin.class), new Runnable() {
                @Override
                public void run() {
                    updateScoreboard();
                }
            },0L,21L);
        }
    }

    @Override
    public void addLines(ScoreboardLine line, ScoreboardLine... lines) {
        this.lines.add(line);
        this.lines.addAll(Arrays.asList(lines));

        if(line instanceof EventLine && !eventLinePreset){
            eventLinePreset = true;
            this.eventTaskID=Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(GAPIPlugin.getPlugin(GAPIPlugin.class), new Runnable() {
                @Override
                public void run() {
                    updateScoreboard();
                }
            },0L,21L);
        }

        createEventLineTask(lines);
    }

    public GamePlayer getPlayer() {
        return player;
    }
}

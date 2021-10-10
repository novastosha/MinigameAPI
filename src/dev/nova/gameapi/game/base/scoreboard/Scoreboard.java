package dev.nova.gameapi.game.base.scoreboard;

import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.UUID;

public interface Scoreboard {

    ChatColor getTheme();
    void setTheme(ChatColor theme);

    String getTitle();
    void setTitle(String title);

    ScoreboardLine[] getScoreboardLines();

    org.bukkit.scoreboard.Scoreboard getBukkitScoreboard();

    void addPlayer(GamePlayer player);
    void removePlayer(GamePlayer player);

    void updateScoreboard();

    void setScoreboardLine(ScoreboardLine line);
    void setScoreboardLines(ScoreboardLine[] lines);
    void addLine(ScoreboardLine line);
    void addLines(ScoreboardLine line,ScoreboardLine... lines);

}

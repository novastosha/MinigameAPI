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

public class Scoreboard {

    private final ChatColor theme;
    private org.bukkit.scoreboard.Scoreboard bukkitScoreboard;
    private String objName;
    private Objective objective;
    private String title;
    private ChatColor cyclePassedColor;
    private ChatColor cycleColor;
    private final ArrayList<GamePlayer> players;
    private String[] lines;

    public Scoreboard(String title,ChatColor theme){
        this.title = title;
        this.theme = theme;
        this.cycleColor = null;
        this.cyclePassedColor = null;
        this.players = new ArrayList<>();
        this.lines = new String[0];

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        this.bukkitScoreboard = manager.getNewScoreboard();

        this.objName = UUID.randomUUID().toString().split("-")[0];
        this.objective = bukkitScoreboard.registerNewObjective(objName, "dummy");

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(title);
    }

    public ChatColor getTheme() {
        return theme;
    }

    public ArrayList<GamePlayer> getPlayers() {
        return players;
    }

    public String getTitle() {
        return title;
    }

    public String[] getLines() {
        return lines;
    }

    public void setLines(String[] lines){
        this.lines = lines;
        updateScoreboard();
    }

    public void setLine(int lineIndex, String line){
        this.lines[lineIndex] = line;
        updateScoreboard();
    }

    public String getLine(int lineIndex){
        return this.lines[lineIndex];
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ChatColor getCycleColor() {
        return cycleColor;
    }

    public ChatColor getCyclePassedColor() {
        return cyclePassedColor;
    }

    public void setCycleColor(ChatColor cycleColor) {
        this.cycleColor = cycleColor;
    }

    public void setCyclePassedColor(ChatColor cyclePassedColor) {
        this.cyclePassedColor = cyclePassedColor;
    }

    public void addPlayer(GamePlayer player){
        this.players.add(player);
    }

    public void removePlayer(GamePlayer player){
        this.players.remove(player);
        player.getPlayer().setScoreboard(null);
    }

    public void updateScoreboard() {
        int index = 0;

        this.objName = UUID.randomUUID().toString().split("-")[0];
        this.objective = bukkitScoreboard.registerNewObjective(objName, "dummy");

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(title);

        Score score1 = objective.getScore(theme+"localhost");
        score1.setScore(index);
        index++;

        for(String line : lines){
            Score score = objective.getScore(line);
            score.setScore(index);
            index++;
        }



        for(GamePlayer player : getPlayers()){
            player.getPlayer().setScoreboard(bukkitScoreboard);
        }
    }

    public void cycle(){

    }
}

package dev.nova.gameapi.game.base.scoreboard;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public interface Scoreboard {

    void addPlayer(Player player);
    void removePlayer(Player player);

    Function<Player,String> getTitle();
    void updateScoreboard(Player player);

    void setLines(ScoreboardLine[] lines);
    Set<ScoreboardLine> getLines();
    List<Player> getPlayers();

    default void updateScoreboard() {
        for(Player player : getPlayers()) {
            updateScoreboard(player);
        }
    }

}

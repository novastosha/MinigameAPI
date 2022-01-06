package dev.nova.gameapi.game.base.scoreboard;

import org.bukkit.entity.Player;

import java.util.function.Function;

public interface ScoreboardLine {

    int getIndex();
    Function<Player, String> getContent();

}

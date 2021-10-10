package dev.nova.gameapi.game.base.scoreboard;

import dev.nova.gameapi.game.player.GamePlayer;

public interface ScoreboardLine {

    int getIndex();
    String getAsString(Scoreboard scoreboard);
}

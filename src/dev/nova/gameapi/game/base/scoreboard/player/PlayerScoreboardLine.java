package dev.nova.gameapi.game.base.scoreboard.player;

import dev.nova.gameapi.game.base.scoreboard.Scoreboard;
import dev.nova.gameapi.game.base.scoreboard.ScoreboardLine;
import dev.nova.gameapi.game.player.GamePlayer;

import java.util.function.Function;

public class PlayerScoreboardLine implements ScoreboardLine {

    private final int index;
    private final Function<GamePlayer, String> lineFunction;

    public PlayerScoreboardLine(int index, Function<GamePlayer, String> lineFunction) {
            this.index = index;
            this.lineFunction = lineFunction;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getAsString(Scoreboard scoreboard) {
        return lineFunction.apply(((PlayerScoreboard)scoreboard).getPlayer());
    }
}

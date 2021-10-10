package dev.nova.gameapi.game.base.scoreboard.game;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.scoreboard.Scoreboard;
import dev.nova.gameapi.game.base.scoreboard.ScoreboardLine;

import java.util.function.Function;

public class GameScoreboardLine implements ScoreboardLine {

    private final int index;
    private final Function<GameInstance, String> lineFunction;

    public GameScoreboardLine(int index, Function<GameInstance, String> lineFunction) {
        this.index = index;
        this.lineFunction = lineFunction;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getAsString(Scoreboard scoreboard) {
        return lineFunction.apply(((GameScoreboard)scoreboard).getGame());
    }
}

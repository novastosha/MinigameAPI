package dev.nova.gameapi.game.base.scoreboard.normal;

import org.bukkit.entity.Player;
import dev.nova.gameapi.game.base.scoreboard.ScoreboardLine;

import java.util.function.Function;

public class NormalScoreboardLine implements ScoreboardLine {

    private final int index;
    private final Function<Player, String> content;

    public NormalScoreboardLine(int index, Function<Player, String> content){
        this.index = index;
        this.content = content;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public Function<Player, String> getContent() {
        return content;
    }
}

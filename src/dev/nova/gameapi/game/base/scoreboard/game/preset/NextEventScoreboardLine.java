package dev.nova.gameapi.game.base.scoreboard.game.preset;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.events.GameEvent;
import dev.nova.gameapi.game.base.instance.events.scoreboard.EventLine;
import dev.nova.gameapi.game.base.scoreboard.game.GameScoreboardLine;
import org.bukkit.ChatColor;

import java.util.function.Function;

public class NextEventScoreboardLine extends GameScoreboardLine implements EventLine {
    public NextEventScoreboardLine(int index, ChatColor eventNameColor, ChatColor timeLeftColor) {
        super(index, new Function<GameInstance, String>() {
            @Override
            public String apply(GameInstance game) {
                try {
                    GameEvent event = game.getEvents().get(game.getCurrentEventIndex() + 1);


                    if (event == null) {
                        event = game.getCurrentEvent();
                    }

                    if (event != null) {
                        return eventNameColor + event.displayName + ": " + timeLeftColor + formatTime(game.getCurrentEvent().durationLeft * 1000L);
                    } else {
                        return "";
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    GameEvent event = game.getCurrentEvent();

                    if (event != null) {
                        return eventNameColor + event.displayName + ": " + timeLeftColor + formatTime(game.getCurrentEvent().durationLeft * 1000L);
                    } else {
                        return "";
                    }
                }
            }
        });
    }

    private static String formatTime(long millis) {
        long secs = millis / 1000;
        return String.format("%02d:%02d", (secs % 3600) / 60, secs % 60);
    }
}

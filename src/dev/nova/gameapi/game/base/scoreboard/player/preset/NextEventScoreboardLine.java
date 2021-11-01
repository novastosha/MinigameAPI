package dev.nova.gameapi.game.base.scoreboard.player.preset;

import dev.nova.gameapi.game.base.instance.events.GameEvent;
import dev.nova.gameapi.game.base.instance.events.scoreboard.EventLine;
import dev.nova.gameapi.game.base.scoreboard.Scoreboard;
import dev.nova.gameapi.game.base.scoreboard.player.PlayerScoreboardLine;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.ChatColor;

import java.util.function.Function;

public class NextEventScoreboardLine extends PlayerScoreboardLine implements EventLine {
    public NextEventScoreboardLine(int index, ChatColor eventNameColor, ChatColor timeLeftColor) {
        super(index, new Function<GamePlayer, String>() {
            @Override
            public String apply(GamePlayer gamePlayer) {
                try {
                    
                    GameEvent event = gamePlayer.getGame().getEvents().get(gamePlayer.getGame().getCurrentEventIndex() + 1);
                   
                    if (event == null) {
                        event = gamePlayer.getGame().getCurrentEvent();
                    }

                    if (event != null) {
                        return eventNameColor + event.displayName + ": " + timeLeftColor + formatTime(gamePlayer.getGame().getCurrentEvent().durationLeft * 1000L);
                    } else {
                        return "";
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    GameEvent event = gamePlayer.getGame().getCurrentEvent();

                    if (event != null) {
                        return eventNameColor + event.displayName + ": " + timeLeftColor + formatTime(gamePlayer.getGame().getCurrentEvent().durationLeft * 1000L);
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

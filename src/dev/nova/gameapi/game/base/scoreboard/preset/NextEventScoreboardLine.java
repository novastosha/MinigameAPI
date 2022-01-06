package dev.nova.gameapi.game.base.scoreboard.preset;

import dev.nova.gameapi.game.base.instance.events.GameEvent;
import dev.nova.gameapi.game.base.instance.events.scoreboard.EventLine;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import dev.nova.gameapi.game.base.scoreboard.normal.NormalScoreboardLine;

import java.util.function.Function;

public class NextEventScoreboardLine extends NormalScoreboardLine implements EventLine {
    public NextEventScoreboardLine(int index, ChatColor eventNameColor, ChatColor timeLeftColor) {
        super(index, new Function<Player, String>() {
            @Override
            public String apply(Player player) {
                try {

                    GamePlayer gamePlayer = GamePlayer.getPlayer(player);
                    GameEvent event = gamePlayer.getGame().getEvents().get(gamePlayer.getGame().getCurrentEventIndex() + 1);

                    if (event != null) {
                        return eventNameColor + event.displayName + ": " + timeLeftColor + formatTime(event.durationLeft * 1000L);
                    } else {
                        return "";
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    return "";
                }
            }
        });
    }


    private static String formatTime(long millis) {
        long secs = millis / 1000;
        return String.format("%02d:%02d", (secs % 3600) / 60, secs % 60);
    }
}

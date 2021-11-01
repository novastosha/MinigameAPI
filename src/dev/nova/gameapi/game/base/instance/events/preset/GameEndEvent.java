package dev.nova.gameapi.game.base.instance.events.preset;

import dev.nova.gameapi.game.base.instance.events.GameEvent;

import java.util.concurrent.TimeUnit;

public class GameEndEvent extends GameEvent {
    public GameEndEvent(int duration) {
        super("end", "Game End", duration);
    }

    @Override
    public void onStart() {
        String formatted = "";
        if (duration > 60) {
            formatted = String.format("%d min, %d sec",
                    TimeUnit.MILLISECONDS.toMinutes(duration * 1000L),
                    TimeUnit.MILLISECONDS.toSeconds(duration * 1000L) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration * 1000L))
            );
        } else if (duration == 60) {
            formatted = "1 minute";
        } else {
            formatted = String.format("%d sec",
                    TimeUnit.MILLISECONDS.toSeconds(duration * 1000L) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration * 1000L))
            );
        }
        if (duration != 0) {
            instance.getUtils().sendMessage("§c§lThe game is ending in: " + formatted);
        }
    }

    @Override
    public void onTick() {
    }

    @Override
    public void onEnd() {
        instance.onEnd();
    }
}

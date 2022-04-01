package dev.nova.gameapi.game.base.instance.events.preset;

import dev.nova.gameapi.game.base.instance.events.GameEvent;

import java.util.concurrent.TimeUnit;

public class GameEndEvent extends GameEvent {
    public GameEndEvent(int duration) {
        super("end", "Game End", duration);
    }

    @Override
    public void onEvent() {
        instance.onEnd(false);
    }
}

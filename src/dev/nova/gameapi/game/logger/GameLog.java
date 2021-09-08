package dev.nova.gameapi.game.logger;

import dev.nova.gameapi.game.base.GameBase;

public class GameLog {

    private final String message;
    private final boolean infromConsole;
    private final LogLevel level;
    private final GameBase game;

    public GameLog(GameBase game, LogLevel level, String message,boolean informConsole){
        this.game = game;
        this.level = level;
        this.message = message;
        this.infromConsole = informConsole;
    }

    public GameBase getGame() {
        return game;
    }

    public LogLevel getLevel() {
        return level;
    }

    public boolean getInfromConsole() {
        return infromConsole;
    }

    public String getMessage() {
        return message;
    }
}

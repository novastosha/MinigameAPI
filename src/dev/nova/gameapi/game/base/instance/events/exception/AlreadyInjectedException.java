package dev.nova.gameapi.game.base.instance.events.exception;

/**
 *
 * An exception that gets thrown when an event class instance is already injected into another game!
 *
 */
public class AlreadyInjectedException extends Exception {
    public AlreadyInjectedException(String s) {
        super(s);
    }
}

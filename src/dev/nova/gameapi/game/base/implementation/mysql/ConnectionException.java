package dev.nova.gameapi.game.base.implementation.mysql;

public class ConnectionException extends Exception {

    public ConnectionException(Exception e){
        super(e);
    }

}

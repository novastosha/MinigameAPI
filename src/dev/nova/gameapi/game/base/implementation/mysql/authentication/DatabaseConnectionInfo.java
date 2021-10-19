package dev.nova.gameapi.game.base.implementation.mysql.authentication;

public record DatabaseConnectionInfo(String username, String password, int port,
                                     String host) {

}

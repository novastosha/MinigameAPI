package dev.nova.gameapi.game.base.implementation.mysql;

import dev.nova.gameapi.game.base.implementation.mysql.authentication.DatabaseConnectionInfo;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnector {

    public final Connection connection;

    public DatabaseConnector(DatabaseConnectionInfo info, String database) throws ConnectionException {
        try{
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection(
                    "jdbc:mysql://"+info.host()+":"+info.port()+"/"+database,info.username(), info.password());
        }catch(Exception e){
            throw new ConnectionException(e);
        }
    }

}


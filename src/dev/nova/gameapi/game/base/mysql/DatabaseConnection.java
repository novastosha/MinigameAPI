package dev.nova.gameapi.game.base.mysql;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    public final String host;
    public final int port;
    public final String username;
    public final String password;
    public final String database;
    public final Connection connection;
    public final String name;

    public DatabaseConnection(String host, int port, String username, String password, String database,String name){
        this.host = host;
        this.name = name;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
        this.connection = makeConnection();
    }

    private Connection makeConnection() {
        try{
            Class.forName("com.mysql.jdbc.Driver");

            return DriverManager.getConnection(
                    "jdbc:mysql://"+host+":"+port+"/"+database,username,password);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}

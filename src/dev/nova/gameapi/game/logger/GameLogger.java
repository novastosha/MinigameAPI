package dev.nova.gameapi.game.logger;

import dev.nova.gameapi.utils.Files;
import org.bukkit.Bukkit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GameLogger {

    public static void log(GameLog log){
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yy-MM-dd");
            LocalDateTime now = LocalDateTime.now();
            File toLog = new File(Files.LOG_FOLDER.getFile(),log.getGame().getCodeName()+"_"+dtf.format(now)+"_log.txt");

            if(!toLog.exists()){
                toLog.createNewFile();
            }


            BufferedWriter writer = new BufferedWriter(new FileWriter(toLog, true));

            writer.write("\n["+log.getLevel().name()+"] "+log.getMessage());

            writer.flush();
            writer.close();

            if(log.getInfromConsole()){
                Bukkit.getServer().getConsoleSender().sendMessage("["+log.getLevel().name()+"] "+"["+log.getGame().getCodeName()+"] "+log.getMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to log!");
        }

    }

}

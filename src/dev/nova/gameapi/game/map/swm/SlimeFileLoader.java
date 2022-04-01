package dev.nova.gameapi.game.map.swm;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.NotDirectoryException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * This class / code is not made by MinigameAPI's makers
 * <p>
 * This class is derived from ASWM's FileLoader implementation <a href="https://github.com/Paul19988/Advanced-Slime-World-Manager/blob/develop/slimeworldmanager-plugin/src/main/java/com/grinderwolf/swm/plugin/loaders/file/FileLoader.java"></a>
 * <p>
 * Use permitted under the GPLv3 license, see: https://opensource.org/licenses/GPL-3.0
 *
 * @author ASWM Contributors
 */
public class SlimeFileLoader implements SlimeLoader {

    private static final FilenameFilter WORLD_FILE_FILTER = (dir, name) -> name.endsWith(".slime");

    private final Map<String, RandomAccessFile> worldFiles = Collections.synchronizedMap(new HashMap<>());
    private final File worldDir;

    public SlimeFileLoader(File worldDir) {
        this.worldDir = worldDir;

        if (worldDir.exists() && !worldDir.isDirectory()) {
            worldDir.delete();
        }

        worldDir.mkdirs();
    }

    @Override
    public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException, IOException{
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        RandomAccessFile file = worldFiles.computeIfAbsent(worldName, (world) -> {

            try {
                return new RandomAccessFile(new File(worldDir, worldName + ".slime"), "rw");
            } catch (FileNotFoundException ex) {
                return null; // This is never going to happen as we've just checked if the world exists
            }

        });

        if(!readOnly) {
            if(file != null && file.getChannel().isOpen()) {
                System.out.print("World is unlocked");
            }
        }

        if(file != null && file.length() > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("World is too big!");
        }

        byte[] serializedWorld = new byte[0];
        if(file != null) {
            serializedWorld = new byte[(int) file.length()];
            file.seek(0); // Make sure we're at the start of the file
            file.readFully(serializedWorld);
        }

        return serializedWorld;
    }

    @Override
    public boolean worldExists(String worldName) {
        return new File(worldDir, worldName + ".slime").exists();
    }

    @Override
    public List<String> listWorlds() throws NotDirectoryException {
        String[] worlds = worldDir.list(WORLD_FILE_FILTER);

        if(worlds == null) {
            throw new NotDirectoryException(worldDir.getPath());
        }

        return Arrays.stream(worlds).map((c) -> c.substring(0, c.length() - 6)).collect(Collectors.toList());
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld, boolean lock) throws IOException {
        RandomAccessFile worldFile = worldFiles.get(worldName);
        boolean tempFile = worldFile == null;

        if (tempFile) {
            worldFile = new RandomAccessFile(new File(worldDir, worldName + ".slime"), "rw");
        }

        worldFile.seek(0); // Make sure we're at the start of the file
        worldFile.setLength(0); // Delete old data
        worldFile.write(serializedWorld);

        if (lock) {
            FileChannel channel = worldFile.getChannel();

            try {
                channel.tryLock();
            } catch (OverlappingFileLockException ignored) {

            }
        }

        if (tempFile) {
            worldFile.close();
        }
    }

    @Override
    public void unlockWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        RandomAccessFile file = worldFiles.remove(worldName);

        if(file != null) {
            FileChannel channel = file.getChannel();
            if(channel.isOpen()) {
                file.close();
            }
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws IOException {
        RandomAccessFile file = worldFiles.get(worldName);

        if (file == null) {
            file = new RandomAccessFile(new File(worldDir, worldName + ".slime"), "rw");
        }

        if(file.getChannel().isOpen()) {
            file.close();
        }else{
            return true;
        }
        return false;
    }

    @Override
    public void deleteWorld(String worldName) throws UnknownWorldException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }else {
            try (RandomAccessFile randomAccessFile = worldFiles.get(worldName)) {

                if(randomAccessFile != null) {

                    randomAccessFile.seek(0); // Make sure we're at the start of the file
                    randomAccessFile.setLength(0); // Delete old data
                    randomAccessFile.close();

                    worldFiles.remove(worldName);
                }

                new File(worldDir, worldName + ".slime").delete();

            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
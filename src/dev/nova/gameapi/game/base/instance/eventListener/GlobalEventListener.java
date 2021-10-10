package dev.nova.gameapi.game.base.instance.eventListener;

import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class GlobalEventListener implements Listener {

    public GlobalEventListener(JavaPlugin plugin, boolean ignoreCancelled) {
        RegisteredListener registeredListener = new RegisteredListener(this, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) throws EventException {
                onEvent(event);
            }
        }, EventPriority.HIGHEST, plugin, ignoreCancelled);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (HandlerList handler : HandlerList.getHandlerLists()) {
                    if(!contains(registeredListener,handler.getRegisteredListeners())) {
                        handler.register(registeredListener);
                    }
                }
            }
        },0L,0L);
    }

    private boolean contains(RegisteredListener registeredListener, RegisteredListener[] registeredListeners) {
        for(RegisteredListener listener : registeredListeners){
            if(listener == registeredListener){
                return true;
            }
        }
        return false;

    }

    protected abstract void onEvent(Event event);
}

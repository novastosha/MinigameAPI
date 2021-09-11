package dev.nova.gameapi.game.base.instance.eventListener;

import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class GlobalListener implements Listener {

    public GlobalListener(JavaPlugin plugin, boolean ignoreCancelled) {
        RegisteredListener registeredListener = new RegisteredListener(this, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) throws EventException {
                onEvent(event);
            }
        }, EventPriority.NORMAL, plugin, ignoreCancelled);
        for (HandlerList handler : HandlerList.getHandlerLists()) {
            handler.register(registeredListener);
        }
    }

    protected abstract void onEvent(Event event);



}

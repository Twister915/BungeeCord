package net.md_5.bungee.event;

/**
 * Create an instance of this and register with the plugin manager to catch any event.
 *
 * Event executors get priority over event handler methods
 */
public interface EventExecutor {
    void handleEvent(Object event);
}

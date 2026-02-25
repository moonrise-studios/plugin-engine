package games.negative.engine.command;

import org.incendo.cloud.CommandManager;

public interface CloudCommand<T> {

    default void onRegister(CommandManager<T> commandManager) {

    }

}

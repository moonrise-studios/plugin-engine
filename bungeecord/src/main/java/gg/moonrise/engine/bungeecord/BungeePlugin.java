package gg.moonrise.engine.bungeecord;

import gg.moonrise.engine.Plugin;
import gg.moonrise.engine.message.util.MiniMessageUtil;
import gg.moonrise.engine.state.Reloadable;
import gg.moonrise.moss.bungee.MossBungee;
import lombok.extern.slf4j.Slf4j;
import net.md_5.bungee.api.plugin.Listener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public abstract class BungeePlugin extends MossBungee implements Plugin {

    @Override
    public void loadInitialComponents(AnnotationConfigApplicationContext context) {
        super.loadInitialComponents(context);

        MiniMessageUtil.init();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        invokeBeans(
                Listener.class,
                listener -> getProxy().getPluginManager().registerListener(this, listener),
                (listener, e) -> log.error("Failed to register listener: {}", listener.getClass().getSimpleName(), e)
        );
    }

    @Override
    public Path directory() {
        return getDataFolder().toPath().toAbsolutePath();
    }

    @Override
    public <T> void fetchBeans(Class<T> clazz, Consumer<T> consumer, BiConsumer<T, Exception> onFailure) {
        invokeBeans(clazz, consumer, onFailure);
    }

    @Override
    public <T> void fetchBeans(Class<T> clazz, Consumer<T> consumer) {
        invokeBeans(clazz, consumer);
    }

    @Override
    public void reload() {
        invokeBeans(
                Reloadable.class,
                Reloadable::reload,
                (reloadable, e) -> log.error("Failed to reload {}", reloadable.getClass().getSimpleName(), e)
        );
    }
}

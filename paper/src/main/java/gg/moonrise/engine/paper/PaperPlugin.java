package gg.moonrise.engine.paper;

import gg.moonrise.engine.Plugin;
import gg.moonrise.engine.message.util.MiniMessageUtil;
import gg.moonrise.engine.paper.scheduler.Scheduler;
import gg.moonrise.engine.paper.readiness.PaperServiceReadiness;
import gg.moonrise.engine.state.ServiceReadiness;
import gg.moonrise.engine.state.ServiceReadiness.Failure;
import gg.moonrise.engine.state.Reloadable;
import gg.moonrise.moss.paper.MossPaper;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Listener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Path;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * An abstract base class for Paper plugins that integrates with the Moss framework.
 * This class provides common functionality for managing plugin data directories and fetching beans from the Moss context.
 */
@Slf4j
public abstract class PaperPlugin extends MossPaper implements Plugin {

    private PaperServiceReadiness readinessBinding;

    @Override
    public void loadInitialComponents(AnnotationConfigApplicationContext context) {
        super.loadInitialComponents(context);

        Scheduler.init(this);
        MiniMessageUtil.init();
    }

    @Override
    public void onEnable() {
        ServiceReadiness readiness = CONTEXT.getBeanProvider(ServiceReadiness.class).getIfAvailable();
        if (readiness != null) {
            readinessBinding = PaperServiceReadiness.install(
                    this,
                    readiness,
                    Component.text(getName() + " is unavailable right now. Please contact an administrator."),
                    Duration.ofSeconds(60)
            );
        }
        try {
            super.onEnable();

            invokeBeans(
                    Listener.class,
                    listener -> getServer().getPluginManager().registerEvents(listener, this),
                    (listener, e) -> log.error("Failed to register listener: {}", listener.getClass().getSimpleName(), e)
            );
        } catch (RuntimeException | LinkageError failure) {
            if (readiness != null) {
                try {
                    readiness.expirePending(new Failure("plugin enable failed", failure));
                } catch (RuntimeException reportFailure) {
                    failure.addSuppressed(reportFailure);
                }
            }
            throw failure;
        }
    }

    @Override
    public void onDisable() {
        try {
            if (readinessBinding != null) {
                readinessBinding.close();
            }
        } finally {
            readinessBinding = null;
            super.onDisable();
        }
    }

    @Override
    public Path directory() {
        return getDataPath().toAbsolutePath();
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

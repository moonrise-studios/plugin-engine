package gg.moonrise.engine.velocity;

import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import gg.moonrise.engine.Plugin;
import gg.moonrise.engine.message.util.MiniMessageUtil;
import gg.moonrise.engine.state.Reloadable;
import gg.moonrise.moss.velocity.MossVelocity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public abstract class VelocityPlugin extends MossVelocity implements Plugin {

    private final Path dataDirectory;

    protected VelocityPlugin(ProxyServer server, @DataDirectory Path dataDirectory) {
        super(server);
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath();
    }

    @Override
    public void loadInitialComponents(AnnotationConfigApplicationContext context) {
        super.loadInitialComponents(context);

        MiniMessageUtil.init();
    }

    @Override
    public Path directory() {
        return dataDirectory;
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
                (reloadable, exception) -> log.error(
                        "Failed to reload {}",
                        reloadable.getClass().getSimpleName(),
                        exception
                )
        );
    }
}

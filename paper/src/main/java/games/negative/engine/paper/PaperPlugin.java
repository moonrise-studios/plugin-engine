package games.negative.engine.paper;

import games.negative.engine.Plugin;
import games.negative.engine.message.util.MiniMessageUtil;
import games.negative.engine.paper.scheduler.Scheduler;
import games.negative.moss.paper.MossPaper;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * An abstract base class for Paper plugins that integrates with the Moss framework.
 * This class provides common functionality for managing plugin data directories and fetching beans from the Moss context.
 */
public abstract class PaperPlugin extends MossPaper implements Plugin {

    @Override
    public void loadInitialComponents(AnnotationConfigApplicationContext context) {
        super.loadInitialComponents(context);

        Scheduler.init(this);
        MiniMessageUtil.init();
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

}

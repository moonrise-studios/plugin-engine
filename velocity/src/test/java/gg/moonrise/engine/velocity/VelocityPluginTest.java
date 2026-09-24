package gg.moonrise.engine.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import gg.moonrise.engine.velocity.command.VelocityCommand;
import org.incendo.cloud.annotations.Command;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityPluginTest {

    @TempDir
    Path dataDirectory;

    @Test
    void exposesInjectedDataDirectoryAndRegistersPluginBean() {
        TestPlugin plugin = new TestPlugin(proxyServer(), dataDirectory);

        assertEquals(dataDirectory.toAbsolutePath(), plugin.directory());

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            plugin.loadInitialComponents(context);
            context.refresh();

            assertSame(plugin, context.getBean(VelocityPlugin.class));
        }
    }

    @Test
    void velocityCommandSupportsAnnotatedCommandSourceHandlers() throws NoSuchMethodException {
        Method method = VelocityCommandFixture.class.getDeclaredMethod("execute", CommandSource.class);

        assertTrue(VelocityCommand.class.isAssignableFrom(VelocityCommandFixture.class));
        assertTrue(method.isAnnotationPresent(Command.class));
    }

    private static ProxyServer proxyServer() {
        return (ProxyServer) Proxy.newProxyInstance(
                ProxyServer.class.getClassLoader(),
                new Class<?>[]{ProxyServer.class},
                (proxy, method, args) -> null
        );
    }

    private static final class TestPlugin extends VelocityPlugin {

        private TestPlugin(ProxyServer server, Path dataDirectory) {
            super(server, dataDirectory);
        }
    }

    private static final class VelocityCommandFixture implements VelocityCommand {

        @Command("velocity-test")
        public void execute(CommandSource source) {
        }
    }
}

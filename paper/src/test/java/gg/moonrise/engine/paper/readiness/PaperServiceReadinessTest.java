package gg.moonrise.engine.paper.readiness;

import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.Component;
import gg.moonrise.engine.state.ServiceReadiness;
import gg.moonrise.engine.state.ServiceReadiness.Failure;
import gg.moonrise.engine.state.ServiceReadiness.FailureAction;
import org.bukkit.Bukkit;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class PaperServiceReadinessTest {

    @Test
    public void deniesBothLoginStagesWhileCriticalRuntimeIsPending() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("chat-runtime", FailureAction.BLOCK_CONNECTIONS)
                .build();
        Component message = Component.text("Unavailable");
        PaperServiceReadiness binding = binding(readiness, message, new ArrayList<>(), new AtomicInteger());
        AsyncPlayerPreLoginEvent preLogin = mock(AsyncPlayerPreLoginEvent.class);
        when(preLogin.getLoginResult()).thenReturn(AsyncPlayerPreLoginEvent.Result.ALLOWED);
        PlayerConnectionValidateLoginEvent login = mock(PlayerConnectionValidateLoginEvent.class);
        when(login.isAllowed()).thenReturn(true);

        binding.onPreLogin(preLogin);
        binding.onValidateLogin(login);
        verify(preLogin).disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, message);
        verify(login).kickMessage(message);
    }

    @Test
    public void keepsAnotherPluginsDenialAndOpensAfterPublication() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("chat-runtime", FailureAction.BLOCK_CONNECTIONS)
                .build();
        Component message = Component.text("Unavailable");
        PaperServiceReadiness binding = binding(readiness, message, new ArrayList<>(), new AtomicInteger());
        AsyncPlayerPreLoginEvent preLogin = mock(AsyncPlayerPreLoginEvent.class);
        when(preLogin.getLoginResult()).thenReturn(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
        binding.onPreLogin(preLogin);
        verify(preLogin, never()).disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, message);

        readiness.ready("chat-runtime");
        PlayerConnectionValidateLoginEvent login = mock(PlayerConnectionValidateLoginEvent.class);
        when(login.isAllowed()).thenReturn(true);
        binding.onValidateLogin(login);
        verify(login, never()).kickMessage(message);
    }

    @Test
    public void finalValidationCatchesAServiceFailureAfterPreLogin() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("chat-runtime", FailureAction.BLOCK_CONNECTIONS)
                .build();
        readiness.ready("chat-runtime");
        Component message = Component.text("Unavailable");
        PaperServiceReadiness binding = binding(readiness, message, new ArrayList<>(), new AtomicInteger());
        AsyncPlayerPreLoginEvent preLogin = mock(AsyncPlayerPreLoginEvent.class);
        when(preLogin.getLoginResult()).thenReturn(AsyncPlayerPreLoginEvent.Result.ALLOWED);
        binding.onPreLogin(preLogin);
        verify(preLogin, never()).disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, message);

        readiness.unavailable("chat-runtime", new Failure("active state lost", new IllegalStateException()));
        PlayerConnectionValidateLoginEvent validation = mock(PlayerConnectionValidateLoginEvent.class);
        when(validation.isAllowed()).thenReturn(true);
        binding.onValidateLogin(validation);
        verify(validation).kickMessage(message);
    }

    @Test
    public void finalValidationPreservesAnExistingDenial() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("chat-runtime", FailureAction.BLOCK_CONNECTIONS)
                .build();
        Component message = Component.text("Unavailable");
        PaperServiceReadiness binding = binding(readiness, message, new ArrayList<>(), new AtomicInteger());
        PlayerConnectionValidateLoginEvent validation = mock(PlayerConnectionValidateLoginEvent.class);
        when(validation.isAllowed()).thenReturn(false);
        binding.onValidateLogin(validation);
        verify(validation, never()).kickMessage(message);
    }

    @Test
    public void staleDisableActionIsCancelledWhenServiceRecoversBeforeItsServerTick() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("config", FailureAction.DISABLE_PLUGIN)
                .build();
        List<Runnable> pending = new ArrayList<>();
        AtomicInteger disabled = new AtomicInteger();
        binding(readiness, Component.text("Unavailable"), pending, disabled);
        readiness.unavailable("config", new Failure("invalid version", new IllegalStateException()));
        assertEquals(1, pending.size());
        readiness.ready("config");
        pending.removeFirst().run();
        assertEquals(0, disabled.get());
    }

    @Test
    public void aFailedCriticalServiceRequestsOneServerStop() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("chat-runtime", FailureAction.STOP_SERVER)
                .build();
        List<Runnable> pending = new ArrayList<>();
        AtomicInteger stopped = new AtomicInteger();
        binding(readiness, Component.text("Unavailable"), pending, new AtomicInteger(), stopped);
        readiness.unavailable("chat-runtime", new Failure("invalid version", new IllegalStateException()));
        assertEquals(1, pending.size());
        pending.removeFirst().run();
        assertEquals(1, stopped.get());
        readiness.unavailable("chat-runtime", new Failure("still unavailable", new IllegalStateException()));
        assertEquals(0, pending.size());
    }

    @Test
    public void featureOnlyStartupDeadlineLogsItsFailureOnce() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("shop-runtime", FailureAction.DISABLE_FEATURE)
                .build();
        Plugin plugin = mock(Plugin.class);
        Logger logger = Logger.getLogger("readiness-feature-test");
        logger.setUseParentHandlers(false);
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { records.add(record); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(handler);
        when(plugin.getLogger()).thenReturn(logger);
        try {
            PaperServiceReadiness.forTesting(plugin, readiness, Component.text("Unavailable"),
                    Runnable::run, () -> { }, ignored -> { });
            Failure failure = new Failure("startup deadline exceeded", new IllegalStateException("late"));
            readiness.expirePending(failure);
            readiness.unavailable("shop-runtime", failure);

            assertEquals(1, records.size());
            assertEquals("Service shop-runtime is unavailable (startup deadline exceeded); feature disabled",
                    records.getFirst().getMessage());
            assertEquals(failure.cause(), records.getFirst().getThrown());
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    public void failureToInstallPluginOnlyReadinessDisablesPluginWithoutStoppingServer() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("shop-runtime", FailureAction.DISABLE_PLUGIN)
                .build();
        Plugin plugin = mock(Plugin.class);
        PluginManager manager = mock(PluginManager.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("readiness-test"));
        doThrow(new IllegalStateException("registration rejected"))
                .when(manager).registerEvents(any(PaperServiceReadiness.class), eq(plugin));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            assertThrows(IllegalStateException.class, () -> PaperServiceReadiness.install(
                    plugin, readiness, Component.empty(), Duration.ofSeconds(30)
            ));
            verify(manager).disablePlugin(plugin);
            bukkit.verify(Bukkit::shutdown, never());
        }
    }

    @Test
    public void failureToInstallTheEarlyGuardRequestsServerShutdown() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("chat-runtime", FailureAction.BLOCK_CONNECTIONS)
                .build();
        Plugin plugin = mock(Plugin.class);
        PluginManager manager = mock(PluginManager.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("readiness-test"));
        doThrow(new IllegalStateException("registration rejected"))
                .when(manager).registerEvents(any(PaperServiceReadiness.class), eq(plugin));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            assertThrows(IllegalStateException.class, () -> PaperServiceReadiness.install(
                    plugin, readiness, Component.text("Unavailable"), Duration.ofSeconds(30)
            ));
            bukkit.verify(Bukkit::shutdown);
        }
    }

    private PaperServiceReadiness binding(
            ServiceReadiness readiness,
            Component message,
            List<Runnable> pending,
            AtomicInteger disabled
    ) {
        return binding(readiness, message, pending, disabled, new AtomicInteger());
    }

    private PaperServiceReadiness binding(
            ServiceReadiness readiness,
            Component message,
            List<Runnable> pending,
            AtomicInteger disabled,
            AtomicInteger stopped
    ) {
        Plugin plugin = mock(Plugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("readiness-test"));
        return PaperServiceReadiness.forTesting(
                plugin, readiness, message, pending::add, stopped::incrementAndGet,
                ignored -> disabled.incrementAndGet()
        );
    }
}

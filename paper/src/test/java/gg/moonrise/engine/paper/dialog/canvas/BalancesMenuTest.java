package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.message.util.MiniMessageUtil;
import gg.moonrise.engine.paper.dialog.canvas.example.BalanceService;
import gg.moonrise.engine.paper.dialog.canvas.example.BalanceSnapshot;
import gg.moonrise.engine.paper.dialog.canvas.example.BalancesMenu;
import gg.moonrise.engine.paper.dialog.canvas.example.Currency;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasRenderer;
import gg.moonrise.engine.paper.dialog.canvas.internal.TextMeasurer;
import gg.moonrise.engine.paper.dialog.canvas.support.TestClickCallbacks;
import gg.moonrise.engine.paper.dialog.canvas.support.TestDialogExecutor;
import gg.moonrise.engine.paper.dialog.canvas.theme.DialogTheme;
import gg.moonrise.engine.paper.dialog.support.TestDialogProviders.TestCustomClickAction;
import gg.moonrise.engine.paper.dialog.support.TestDialogProviders.TestDialog;
import gg.moonrise.engine.paper.dialog.support.TestDialogProviders.TestDialogListType;
import gg.moonrise.engine.paper.dialog.support.TestDialogProviders.TestNoticeType;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BalancesMenuTest extends MockBukkitTest {

    private TestDialogExecutor executor;
    private DialogCanvasRuntime runtime;
    private DialogTheme theme;
    private BalanceService balances;

    @BeforeEach
    public void setUpRuntime() {
        executor = new TestDialogExecutor();
        runtime = new DialogCanvasRuntime(executor);
        DialogCanvasRuntime.install(runtime);
        theme = DialogTheme.builder("example", "balances").width(440).build();
        balances = BalancesMenuTest::snapshot;

        for (int rank = 1; rank <= 16; rank++) {
            runtime.heads().put(playerId(rank), HeadPixels.placeholder());
        }
    }

    @Test
    public void rendersThemedScreenWhereEveryLineHasTheThemeWidth() {
        PlayerMock player = viewer("Viewer");
        DialogSession<BalanceSnapshot> session = new BalancesMenu(balances, theme).open(player, snapshot(player.getUniqueId(), Currency.MONEY));

        assertTrue(session.isOpen());
        TestDialog dialog = assertInstanceOf(TestDialog.class, session.currentDialog());
        DialogBase base = dialog.base();
        assertEquals("Balances", MiniMessageUtil.componentToPlainText(base.title()));
        assertFalse(base.pause());
        assertEquals(DialogBase.DialogAfterAction.NONE, base.afterAction());
        assertTrue(base.canCloseWithEscape());

        PlainMessageDialogBody body = assertInstanceOf(PlainMessageDialogBody.class, base.body().get(0));
        assertEquals(440 + CanvasRenderer.BODY_WRAP_SLACK, body.width());

        List<Component> lines = lines(body.contents());
        TextMeasurer measurer = new TextMeasurer(theme.compiled());
        for (int i = 0; i < lines.size(); i++) {
            assertEquals(440, measurer.width(lines.get(i)), "line " + i);
        }
        // panel, 3 tab lines, 4 rows of 4 lines with 3 gap lines, blank + footer, bottom padding
        assertEquals(1 + 3 + 19 + 2 + 1, lines.size());

        TestNoticeType type = assertInstanceOf(TestNoticeType.class, dialog.type());
        assertEquals("Back", MiniMessageUtil.componentToPlainText(type.action().label()));

        String plain = MiniMessageUtil.componentToPlainText(body.contents());
        assertTrue(plain.contains("Player1"));
        assertTrue(plain.contains("You #42 | 8,406,858"));
    }

    @Test
    public void tabClickShowsOnlyTheNewestLoad() {
        PlayerMock player = viewer("Viewer");
        DialogSession<BalanceSnapshot> session = new BalancesMenu(balances, theme).open(player, snapshot(player.getUniqueId(), Currency.MONEY));

        click(session, "Gems", player);
        click(session, "Pearls", player);
        assertEquals(2, executor.pending());
        assertTrue(session.isLoading());

        long generation = session.generation();
        executor.run(1);
        assertEquals(Currency.PEARLS, session.data().currency());
        assertEquals(generation + 1, session.generation());

        executor.runAll();
        assertEquals(Currency.PEARLS, session.data().currency());
        assertEquals(generation + 1, session.generation());
        assertFalse(session.isLoading());
    }

    @Test
    public void slowerOlderLoadIsDiscardedWhenItFinishesFirst() {
        PlayerMock player = viewer("Viewer");
        DialogSession<BalanceSnapshot> session = new BalancesMenu(balances, theme).open(player, snapshot(player.getUniqueId(), Currency.MONEY));

        click(session, "Gems", player);
        click(session, "Pearls", player);
        executor.run(0);
        assertEquals(Currency.MONEY, session.data().currency());

        executor.runAll();
        assertEquals(Currency.PEARLS, session.data().currency());
    }

    @Test
    public void actionsFromReplacedScreensAreInert() {
        PlayerMock player = viewer("Viewer");
        DialogSession<BalanceSnapshot> session = new BalancesMenu(balances, theme).open(player, snapshot(player.getUniqueId(), Currency.MONEY));
        ClickEvent staleGems = clicks(session, "Gems").get(0);

        click(session, "Pearls", player);
        executor.runAll();

        TestClickCallbacks.click(staleGems, player);
        assertEquals(0, executor.pending());
    }

    @Test
    public void actionsIgnoreOtherPlayers() {
        PlayerMock player = viewer("Viewer");
        PlayerMock other = viewer("Other");
        DialogSession<BalanceSnapshot> session = new BalancesMenu(balances, theme).open(player, snapshot(player.getUniqueId(), Currency.MONEY));

        TestClickCallbacks.click(clicks(session, "Gems").get(0), other);
        assertEquals(0, executor.pending());
    }

    @Test
    public void exitButtonEndsTheSession() {
        PlayerMock player = viewer("Viewer");
        DialogSession<BalanceSnapshot> session = new BalancesMenu(balances, theme).open(player, snapshot(player.getUniqueId(), Currency.MONEY));

        TestNoticeType type = assertInstanceOf(TestNoticeType.class, ((TestDialog) session.currentDialog()).type());
        TestCustomClickAction exit = assertInstanceOf(TestCustomClickAction.class, type.action().action());
        exit.callback().accept(null, player);

        assertFalse(session.isOpen());
        assertTrue(runtime.sessions().active(player.getUniqueId()).isEmpty());
    }

    @Test
    public void loadsFinishingAfterCloseAreDiscarded() {
        PlayerMock player = viewer("Viewer");
        DialogSession<BalanceSnapshot> session = new BalancesMenu(balances, theme).open(player, snapshot(player.getUniqueId(), Currency.MONEY));
        long generation = session.generation();

        click(session, "Gems", player);
        session.close();
        executor.runAll();

        assertEquals(Currency.MONEY, session.data().currency());
        assertEquals(generation, session.generation());
    }

    @Test
    public void openingAnotherMenuReplacesTheSession() {
        PlayerMock player = viewer("Viewer");
        AtomicReference<DialogCloseReason> closed = new AtomicReference<>();
        DialogMenu<String> first = new DialogMenu<>("First", theme) {
            @Override
            protected void render(DialogSession<String> session, DialogCanvas ui) {
                ui.text(session.data());
            }

            @Override
            protected void onClose(DialogSession<String> session, DialogCloseReason reason) {
                closed.set(reason);
            }
        };

        DialogSession<String> previous = first.open(player, "hello");
        DialogSession<BalanceSnapshot> next = new BalancesMenu(balances, theme).open(player, snapshot(player.getUniqueId(), Currency.MONEY));

        assertFalse(previous.isOpen());
        assertEquals(DialogCloseReason.REPLACED, closed.get());
        assertSame(next, runtime.sessions().active(player.getUniqueId()).orElseThrow());
        assertInstanceOf(TestDialogListType.class, ((TestDialog) previous.currentDialog()).type());
    }

    @Test
    public void missingResourcePackFallsBackToAVanillaDialog() {
        PlayerMock player = server.addPlayer("NoPack");
        DialogSession<BalanceSnapshot> session = new BalancesMenu(balances, theme).open(player, snapshot(player.getUniqueId(), Currency.MONEY));

        assertTrue(session.isOpen());
        assertEquals(DialogPresentation.NATIVE, session.presentation());
        assertNotNull(session.currentDialog());
    }

    @Test
    public void quittingEndsTheSession() {
        runtime.register(MockBukkit.createMockPlugin());
        PlayerMock player = viewer("Viewer");
        DialogSession<BalanceSnapshot> session = new BalancesMenu(balances, theme).open(player, snapshot(player.getUniqueId(), Currency.MONEY));

        player.disconnect();

        assertFalse(session.isOpen());
        assertTrue(runtime.packs().loadedPacks(player.getUniqueId()).isEmpty());
    }

    @Test
    public void canvasTooTallForTheThemeShowsAVanillaDialogInstead() {
        PlayerMock player = viewer("Viewer");
        DialogTheme small = DialogTheme.builder("example", "small").maxLines(5).build();
        runtime.packs().markLoaded(player.getUniqueId(), small.compiled().packId());
        DialogMenu<Void> menu = new DialogMenu<>("Tall", small) {
            @Override
            protected void render(DialogSession<Void> session, DialogCanvas ui) {
                ui.spacer(10);
            }
        };

        DialogSession<Void> session = menu.open(player, null);

        assertTrue(session.isOpen());
        assertEquals(DialogPresentation.THEMED, session.presentation());
        TestDialog dialog = assertInstanceOf(TestDialog.class, session.currentDialog());
        assertInstanceOf(TestDialogListType.class, dialog.type());
        assertTrue(dialog.base().body().isEmpty(), "the vanilla dialog has no themed panel body");
    }

    @Test
    public void lineBreaksInsideSingleLineTextDoNotShiftLaterLines() {
        PlayerMock player = viewer("Viewer");
        DialogMenu<Void> menu = new DialogMenu<>("Breaks", theme) {
            @Override
            protected void render(DialogSession<Void> session, DialogCanvas ui) {
                ui.text(Component.text("first\nsecond"));
                ui.button(Component.text("two\nlines"), () -> {
                });
                ui.footerText("footer<newline>more");
            }
        };

        DialogSession<Void> session = menu.open(player, null);
        PlainMessageDialogBody body = (PlainMessageDialogBody) ((TestDialog) session.currentDialog()).base().body().get(0);
        List<Component> lines = lines(body.contents());
        TextMeasurer measurer = new TextMeasurer(theme.compiled());

        assertEquals(1 + 1 + 3 + 2 + 1, lines.size());
        for (Component line : lines) {
            assertFalse(MiniMessageUtil.componentToPlainText(line).contains("\n"));
            assertEquals(440, measurer.width(line));
        }
    }

    @Test
    public void sweepClosesExpiredSessions() throws InterruptedException {
        PlayerMock player = viewer("Viewer");
        AtomicReference<DialogCloseReason> closed = new AtomicReference<>();
        DialogMenu<Void> menu = new DialogMenu<>("Short", theme) {
            @Override
            protected void render(DialogSession<Void> session, DialogCanvas ui) {
                ui.text("hello");
            }

            @Override
            protected java.time.Duration sessionTimeout() {
                return java.time.Duration.ofMillis(1);
            }

            @Override
            protected void onClose(DialogSession<Void> session, DialogCloseReason reason) {
                closed.set(reason);
            }
        };

        DialogSession<Void> session = menu.open(player, null);
        Thread.sleep(5);
        runtime.sessions().sweep();

        assertEquals(DialogCloseReason.EXPIRED, closed.get());
        assertFalse(session.isOpen());
    }

    @Test
    public void tooManyTabsFailLoudly() {
        PlayerMock player = viewer("Viewer");
        DialogMenu<Void> menu = new DialogMenu<>("Tabs", theme) {
            @Override
            protected void render(DialogSession<Void> session, DialogCanvas ui) {
                ui.tabs(IntStreamHolder.values(80), 1);
            }
        };

        assertThrows(IllegalStateException.class, () -> menu.open(player, null));
    }

    private static final class IntStreamHolder {

        private static List<Integer> values(int count) {
            List<Integer> values = new ArrayList<>();
            for (int i = 0; i < count; i++) values.add(i);
            return values;
        }
    }

    @Test
    public void inlineCanvasBuilderOpensWithoutSubclass() {
        PlayerMock player = viewer("Viewer");

        DialogSession<Void> session = gg.moonrise.engine.paper.dialog.Dialogs.canvas(player)
                .theme(theme)
                .title("<gold>Rewards")
                .render((current, ui) -> {
                    ui.text("<yellow>Daily reward ready");
                    ui.divider();
                    ui.button("<green>Claim", () -> {
                    }).tooltip("Claim it");
                    ui.image(DialogTheme.BACK);
                    ui.paragraph("A long description that needs to wrap across more than one line of the themed panel for sure, really.");
                })
                .open();

        TestDialog dialog = assertInstanceOf(TestDialog.class, session.currentDialog());
        assertEquals("Rewards", MiniMessageUtil.componentToPlainText(dialog.base().title()));
        assertInstanceOf(TestDialogListType.class, dialog.type());
        assertEquals(1, clicks(session, "Claim").size());
    }

    private PlayerMock viewer(String name) {
        PlayerMock player = server.addPlayer(name);
        runtime.packs().markLoaded(player.getUniqueId(), theme.compiled().packId());
        return player;
    }

    private static void click(DialogSession<?> session, String label, PlayerMock player) {
        TestClickCallbacks.click(clicks(session, label).get(0), player);
    }

    private static List<ClickEvent> clicks(DialogSession<?> session, String label) {
        TestDialog dialog = (TestDialog) session.currentDialog();
        PlainMessageDialogBody body = (PlainMessageDialogBody) dialog.base().body().get(0);
        return TestClickCallbacks.clicksOnText(body.contents(), label);
    }

    private static List<Component> lines(Component body) {
        List<Component> lines = new ArrayList<>();
        for (Component child : body.children()) {
            if (!child.equals(Component.newline())) lines.add(child);
        }

        return lines;
    }

    private static BalanceSnapshot snapshot(UUID viewer, Currency currency) {
        List<BalanceSnapshot.Entry> top = new ArrayList<>();
        for (int rank = 1; rank <= 16; rank++) {
            top.add(new BalanceSnapshot.Entry(rank, playerId(rank), "Player" + rank, String.format("%,d", (17 - rank) * 1_000_000L)));
        }

        return new BalanceSnapshot(currency, top, "You #42 | 8,406,858");
    }

    private static UUID playerId(int rank) {
        return UUID.nameUUIDFromBytes(("player-" + rank).getBytes(StandardCharsets.UTF_8));
    }
}

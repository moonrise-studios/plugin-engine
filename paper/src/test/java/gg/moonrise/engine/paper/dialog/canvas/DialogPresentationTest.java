package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.message.util.MiniMessageUtil;
import gg.moonrise.engine.paper.dialog.canvas.example.BalanceSnapshot;
import gg.moonrise.engine.paper.dialog.canvas.example.BalancesMenu;
import gg.moonrise.engine.paper.dialog.canvas.example.Currency;
import gg.moonrise.engine.paper.dialog.canvas.internal.TextMeasurer;
import gg.moonrise.engine.paper.dialog.canvas.support.TestBedrockForms;
import gg.moonrise.engine.paper.dialog.canvas.support.TestClickCallbacks;
import gg.moonrise.engine.paper.dialog.canvas.support.TestDialogExecutor;
import gg.moonrise.engine.paper.dialog.canvas.theme.DialogTheme;
import gg.moonrise.engine.paper.dialog.canvas.theme.ThemeAsset;
import gg.moonrise.engine.paper.dialog.support.TestDialogProviders.TestCustomClickAction;
import gg.moonrise.engine.paper.dialog.support.TestDialogProviders.TestDialog;
import gg.moonrise.engine.paper.dialog.support.TestDialogProviders.TestNoticeType;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.type.MultiActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DialogPresentationTest extends MockBukkitTest {

    private TestDialogExecutor executor;
    private TestBedrockForms bedrock;
    private DialogCanvasRuntime runtime;
    private DialogTheme theme;
    private DialogCanvasRuntime originalRuntime;

    @BeforeEach
    public void setUpRuntime() {
        executor = new TestDialogExecutor();
        bedrock = new TestBedrockForms();
        runtime = new DialogCanvasRuntime(executor, bedrock);
        originalRuntime = DialogCanvasRuntime.shared();
        DialogCanvasRuntime.install(runtime);
        theme = DialogTheme.builder("example", "balances").width(440).build();

        // BalancesMenu waits for the faces while loading; cached faces keep that off the test executor
        for (int rank = 1; rank <= 16; rank++) {
            runtime.heads().put(playerId(rank), HeadPixels.placeholder());
        }
    }

    @AfterEach
    public void restoreRuntime() {
        DialogCanvasRuntime.install(originalRuntime);
    }

    @Test
    public void javaPlayerWithoutThePackGetsTheSameScreenAsAVanillaDialog() {
        PlayerMock player = server.addPlayer("NoPack");
        DialogSession<BalanceSnapshot> session = balances().open(player, snapshot(Currency.MONEY));

        assertTrue(session.isOpen());
        assertEquals(DialogPresentation.NATIVE, session.presentation());
        TestDialog dialog = assertInstanceOf(TestDialog.class, session.currentDialog());
        MultiActionType type = assertInstanceOf(MultiActionType.class, dialog.type());

        List<String> labels = type.actions().stream().map(button -> plain(button.label())).toList();
        assertEquals(List.of("» Money «", "Gems", "Pearls", "Farm Tokens", "Doubloons"), labels);
        assertEquals(2, type.columns());
        assertEquals("Back", plain(type.exitAction().label()));

        String body = bodies(dialog);
        assertTrue(body.contains("Player1 - 15,000,000"), body);
        assertTrue(body.contains("You #42 | 8,406,858"), body);
    }

    @Test
    public void vanillaDialogButtonsRunTheSameHandlers() {
        PlayerMock player = server.addPlayer("NoPack");
        DialogSession<BalanceSnapshot> session = balances().open(player, snapshot(Currency.MONEY));

        click(button(session, "Gems"), player);
        executor.runAll();

        assertEquals(Currency.GEMS, session.data().currency());
        MultiActionType type = (MultiActionType) ((TestDialog) session.currentDialog()).type();
        assertEquals("» Gems «", plain(type.actions().get(1).label()));
    }

    @Test
    public void staleVanillaButtonsAreInert() {
        PlayerMock player = server.addPlayer("NoPack");
        DialogSession<BalanceSnapshot> session = balances().open(player, snapshot(Currency.MONEY));
        ActionButton staleGems = button(session, "Gems");

        click(button(session, "Pearls"), player);
        executor.runAll();
        click(staleGems, player);

        assertEquals(0, executor.pending());
        assertEquals(Currency.PEARLS, session.data().currency());
    }

    @Test
    public void withoutNativeFallbackAPlayerWithoutThePackIsTold() {
        PlayerMock player = server.addPlayer("NoPack");
        BalancesMenu menu = new BalancesMenu((viewer, currency) -> snapshot(currency), theme) {
            @Override
            protected boolean nativeFallback() {
                return false;
            }
        };

        DialogSession<BalanceSnapshot> session = menu.open(player, snapshot(Currency.MONEY));

        assertFalse(session.isOpen());
        assertNull(session.currentDialog());
        assertTrue(plain(player.nextComponentMessage()).contains("resource pack"));
    }

    @Test
    public void anyLoadedPackCountsWhenTheMenuSaysSo() {
        PlayerMock player = server.addPlayer("ServerPack");
        runtime.packs().markLoaded(player.getUniqueId(), UUID.randomUUID());
        BalancesMenu menu = new BalancesMenu((viewer, currency) -> snapshot(currency), theme) {
            @Override
            protected boolean hasResourcePack(Player viewer) {
                return runtime().packs().isAnyLoaded(viewer);
            }
        };

        assertEquals(DialogPresentation.THEMED, menu.open(player, snapshot(Currency.MONEY)).presentation());
    }

    @Test
    public void bedrockPlayerGetsTheSameScreenAsAForm() {
        PlayerMock player = server.addPlayer("Bedrock");
        bedrock.markBedrock(player.getUniqueId());
        DialogSession<BalanceSnapshot> session = balances().open(player, snapshot(Currency.MONEY));

        assertEquals(DialogPresentation.BEDROCK, session.presentation());
        assertNull(session.currentDialog());
        BedrockForm form = bedrock.last();
        assertTrue(form.title().contains("Balances"));
        assertTrue(form.content().contains("You #42 | 8,406,858"));

        // 5 tabs, 16 face cards, Back
        assertEquals(22, form.buttons().size());
        assertTrue(form.buttons().get(0).text().contains("» "));
        BedrockForm.Button first = form.buttons().get(5);
        assertTrue(first.text().contains("Player1"));
        assertNotNull(first.image());
        assertTrue(first.image().url());
        assertTrue(first.image().data().contains(playerId(1).toString()));
        assertTrue(form.buttons().get(21).text().contains("Back"));
    }

    @Test
    public void bedrockTabClickLoadsAndSendsTheNewScreen() {
        PlayerMock player = server.addPlayer("Bedrock");
        bedrock.markBedrock(player.getUniqueId());
        DialogSession<BalanceSnapshot> session = balances().open(player, snapshot(Currency.MONEY));

        bedrock.last().onClick().accept(bedrock.button("Pearls"));
        assertEquals(1, bedrock.sent().size());
        executor.runAll();

        assertEquals(Currency.PEARLS, session.data().currency());
        assertEquals(2, bedrock.sent().size());
        assertTrue(bedrock.last().buttons().get(2).text().contains("» "));
    }

    @Test
    public void bedrockClickThatShowsNothingSendsTheFormAgain() {
        PlayerMock player = server.addPlayer("Bedrock");
        bedrock.markBedrock(player.getUniqueId());
        DialogSession<BalanceSnapshot> session = balances().open(player, snapshot(Currency.MONEY));

        bedrock.last().onClick().accept(bedrock.button("Player3"));

        assertTrue(session.isOpen());
        assertEquals(2, bedrock.sent().size());
    }

    @Test
    public void closingABedrockFormRunsTheExitButton() {
        PlayerMock player = server.addPlayer("Bedrock");
        bedrock.markBedrock(player.getUniqueId());
        DialogSession<BalanceSnapshot> session = balances().open(player, snapshot(Currency.MONEY));

        bedrock.last().onClose().run();

        assertFalse(session.isOpen());
        assertTrue(runtime.sessions().active(player.getUniqueId()).isEmpty());
    }

    @Test
    public void replacedBedrockFormsIgnoreTheirResponses() {
        PlayerMock player = server.addPlayer("Bedrock");
        bedrock.markBedrock(player.getUniqueId());
        DialogSession<BalanceSnapshot> session = balances().open(player, snapshot(Currency.MONEY));
        BedrockForm stale = bedrock.last();

        stale.onClick().accept(bedrock.button("Gems"));
        executor.runAll();
        stale.onClose().run();
        stale.onClick().accept(bedrock.button("Pearls"));

        assertTrue(session.isOpen());
        assertEquals(0, executor.pending());
        assertEquals(Currency.GEMS, session.data().currency());
    }

    @Test
    public void bedrockFormsCanBeTurnedOff() {
        PlayerMock player = server.addPlayer("Bedrock");
        bedrock.markBedrock(player.getUniqueId());
        BalancesMenu menu = new BalancesMenu((viewer, currency) -> snapshot(currency), theme) {
            @Override
            protected boolean bedrockForms() {
                return false;
            }
        };

        assertFalse(menu.open(player, snapshot(Currency.MONEY)).isOpen());
        assertTrue(bedrock.sent().isEmpty());
        assertTrue(plain(player.nextComponentMessage()).contains("Bedrock"));
    }

    @Test
    public void backButtonKeepsTheSessionAndShowsTheNextPage() {
        PlayerMock player = viewer("Viewer");
        DialogSession<String> session = pages().open(player, "detail");

        TestNoticeType type = assertInstanceOf(TestNoticeType.class, ((TestDialog) session.currentDialog()).type());
        TestCustomClickAction back = assertInstanceOf(TestCustomClickAction.class, type.action().action());
        back.callback().accept(null, player);

        assertTrue(session.isOpen());
        assertEquals("overview", session.data());
    }

    @Test
    public void backButtonThatShowsNothingShowsTheScreenAgain() {
        PlayerMock player = viewer("Viewer");
        AtomicInteger backs = new AtomicInteger();
        DialogMenu<Void> menu = new DialogMenu<>("Stay", theme) {
            @Override
            protected void render(DialogSession<Void> session, DialogCanvas ui) {
                ui.text("hello");
                ui.backButton("Back", backs::incrementAndGet);
            }
        };
        DialogSession<Void> session = menu.open(player, null);
        long generation = session.generation();

        TestNoticeType type = (TestNoticeType) ((TestDialog) session.currentDialog()).type();
        ((TestCustomClickAction) type.action().action()).callback().accept(null, player);

        assertEquals(1, backs.get());
        assertTrue(session.isOpen());
        assertEquals(generation + 1, session.generation());
    }

    @Test
    public void linksOpenPagesOnJavaAndArriveInChatOnBedrock() {
        PlayerMock java = viewer("Java");
        DialogSession<String> themed = pages().open(java, "overview");
        List<ClickEvent> clicks = TestClickCallbacks.clicksOnText(themedBody(themed), "Guide");
        assertFalse(clicks.isEmpty());
        assertEquals(ClickEvent.Action.OPEN_URL, clicks.get(0).action());

        PlayerMock noPack = server.addPlayer("NoPack");
        DialogSession<String> vanilla = pages().open(noPack, "overview");
        DialogAction link = button(vanilla, "Guide").action();
        DialogAction.StaticAction staticAction = assertInstanceOf(DialogAction.StaticAction.class, link);
        assertEquals(ClickEvent.Action.OPEN_URL, staticAction.value().action());

        PlayerMock phone = server.addPlayer("Phone");
        bedrock.markBedrock(phone.getUniqueId());
        pages().open(phone, "overview");
        bedrock.last().onClick().accept(bedrock.button("Guide"));
        assertTrue(plain(phone.nextComponentMessage()).contains("https://example.com/guide"));
        assertEquals(2, bedrock.sent().size());
    }

    private BalancesMenu balances() {
        return new BalancesMenu((viewer, currency) -> snapshot(currency), theme);
    }

    private DialogMenu<String> pages() {
        return new DialogMenu<>("Pages", theme) {
            @Override
            protected void render(DialogSession<String> session, DialogCanvas ui) {
                if (session.data().equals("overview")) {
                    ui.text("Overview");
                    ui.link("Guide", "https://example.com/guide");
                    ui.button("Detail", () -> session.update("detail"));
                    ui.exitButton("Close", session::close);
                } else {
                    ui.paragraph("Detail text");
                    ui.backButton("Back", () -> session.update("overview"));
                }
            }
        };
    }

    private PlayerMock viewer(String name) {
        PlayerMock player = server.addPlayer(name);
        runtime.packs().markLoaded(player.getUniqueId(), theme.compiled().packId());
        return player;
    }

    private static Component themedBody(DialogSession<?> session) {
        return ((PlainMessageDialogBody) ((TestDialog) session.currentDialog()).base().body().get(0)).contents();
    }

    private static ActionButton button(DialogSession<?> session, String label) {
        MultiActionType type = (MultiActionType) ((TestDialog) session.currentDialog()).type();
        return type.actions().stream()
                .filter(button -> plain(button.label()).contains(label))
                .findFirst()
                .orElseThrow();
    }

    private static void click(ActionButton button, Player player) {
        TestCustomClickAction action = assertInstanceOf(TestCustomClickAction.class, button.action());
        action.callback().accept(null, player);
    }

    private static String bodies(TestDialog dialog) {
        List<String> texts = new ArrayList<>();
        for (DialogBody body : dialog.base().body()) {
            texts.add(plain(((PlainMessageDialogBody) body).contents()));
        }

        return String.join("\n", texts);
    }

    private static String plain(Component component) {
        return MiniMessageUtil.componentToPlainText(component);
    }

    private static BalanceSnapshot snapshot(Currency currency) {
        List<BalanceSnapshot.Entry> top = new ArrayList<>();
        for (int rank = 1; rank <= 16; rank++) {
            top.add(new BalanceSnapshot.Entry(rank, playerId(rank), "Player" + rank, String.format("%,d", (16 - rank) * 1_000_000L)));
        }

        return new BalanceSnapshot(currency, top, "You #42 | 8,406,858");
    }

    private static UUID playerId(int rank) {
        return UUID.nameUUIDFromBytes(("player-" + rank).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void aStaleExitFromAnEndedSessionLeavesTheNewerMenuAlone() {
        PlayerMock player = viewer("Viewer");
        DialogSession<String> first = pages().open(player, "overview");
        TestNoticeType type = assertInstanceOf(TestNoticeType.class, ((TestDialog) first.currentDialog()).type());
        TestCustomClickAction staleExit = (TestCustomClickAction) type.action().action();

        DialogSession<String> second = pages().open(player, "overview");
        staleExit.callback().accept(null, player);

        assertFalse(first.isOpen());
        assertTrue(second.isOpen());
    }

    @Test
    public void aFailedLoadSendsTheBedrockFormAgain() {
        PlayerMock player = server.addPlayer("Bedrock");
        bedrock.markBedrock(player.getUniqueId());
        BalancesMenu menu = new BalancesMenu((viewer, currency) -> {
            throw new IllegalStateException("database down");
        }, theme) {
            @Override
            protected void onLoadFailure(DialogSession<BalanceSnapshot> session, Throwable error) {
            }
        };
        DialogSession<BalanceSnapshot> session = menu.open(player, snapshot(Currency.MONEY));

        bedrock.last().onClick().accept(bedrock.button("Gems"));
        assertEquals(1, bedrock.sent().size());
        executor.runAll();

        assertTrue(session.isOpen());
        assertEquals(2, bedrock.sent().size());
    }

    @Test
    public void disablingRunsCloseHooksAtOnce() {
        PlayerMock player = viewer("Viewer");
        AtomicInteger closes = new AtomicInteger();
        DialogMenu<Void> menu = new DialogMenu<>("Hooks", theme) {
            @Override
            protected void render(DialogSession<Void> session, DialogCanvas ui) {
                ui.text("hello");
            }

            @Override
            protected void onClose(DialogSession<Void> session, DialogCloseReason reason) {
                closes.incrementAndGet();
            }
        };
        menu.open(player, null);

        runtime.shutdown();

        assertEquals(1, closes.get());
    }

    @Test
    public void cardBackgroundsSpanTheCardAndShareItsClick() {
        PlayerMock player = viewer("Viewer");
        DialogTheme tiles = DialogTheme.builder("example", "tiles").width(440)
                .asset("tile", ThemeAsset.button(0xFF262636, 0xFF56566E, 40).topOffset(-4))
                .build();
        runtime.packs().markLoaded(player.getUniqueId(), tiles.compiled().packId());
        AtomicInteger clicks = new AtomicInteger();
        DialogMenu<Void> menu = new DialogMenu<>("Tiles", tiles) {
            @Override
            protected void render(DialogSession<Void> session, DialogCanvas ui) {
                ui.centeredRows(DialogLayouts.grid(List.of("A", "B", "C"), 3), (card, label) -> card
                        .background("tile").spacer(3).text(label).onClick(clicks::incrementAndGet)).cardWidth(90);
            }
        };

        DialogSession<Void> session = menu.open(player, null);
        Component body = themedBody(session);
        TestClickCallbacks.click(TestClickCallbacks.clicksOnText(body, "B").get(0), player);

        assertEquals(1, clicks.get());
        assertEquals(440, new TextMeasurer(tiles.compiled())
                .width(((TextComponent) body).children().get(0)));
    }

    @Test
    public void bedrockButtonsShowTheIconTheMenuNamesATextureFor() {
        PlayerMock player = server.addPlayer("Bedrock");
        bedrock.markBedrock(player.getUniqueId());
        DialogMenu<Void> menu = new DialogMenu<>("Icons", theme) {
            @Override
            protected void render(DialogSession<Void> session, DialogCanvas ui) {
                ui.card(card -> card.image(DialogTheme.BACK).text("Back home").onClick(() -> {
                }));
            }

            @Override
            public String bedrockImage(String asset) {
                return asset.equals(DialogTheme.BACK) ? "textures/example/balances/back" : null;
            }
        };

        menu.open(player, null);

        BedrockForm.Image image = bedrock.last().buttons().get(0).image();
        assertFalse(image.url());
        assertEquals("textures/example/balances/back", image.data());
    }

    @Test
    public void clickableCardsWithOnlyAnImageKeepTheirActionOffTheme() {
        AtomicInteger clicks = new AtomicInteger();
        DialogMenu<Void> menu = new DialogMenu<>("Icons", theme) {
            @Override
            protected void render(DialogSession<Void> session, DialogCanvas ui) {
                ui.card(card -> card.image(DialogTheme.BACK).tooltip("Go back").onClick(clicks::incrementAndGet));
            }

            @Override
            public String bedrockImage(String asset) {
                return asset.equals(DialogTheme.BACK) ? "textures/example/balances/back" : null;
            }
        };

        PlayerMock noPack = server.addPlayer("NoPack");
        DialogSession<Void> vanilla = menu.open(noPack, null);
        assertEquals(DialogPresentation.NATIVE, vanilla.presentation());
        click(button(vanilla, "Go back"), noPack);
        assertEquals(1, clicks.get());

        PlayerMock phone = server.addPlayer("Phone");
        bedrock.markBedrock(phone.getUniqueId());
        menu.open(phone, null);
        BedrockForm.Button button = bedrock.last().buttons().get(bedrock.button("Go back"));
        assertEquals("textures/example/balances/back", button.image().data());
        bedrock.last().onClick().accept(bedrock.button("Go back"));
        assertEquals(2, clicks.get());
    }

    @Test
    public void anUnansweredBedrockFormIsClosedBeforeTheNextOneOrWhenReplaced() {
        PlayerMock player = server.addPlayer("Bedrock");
        bedrock.markBedrock(player.getUniqueId());
        DialogSession<BalanceSnapshot> session = balances().open(player, snapshot(Currency.MONEY));
        assertEquals(0, bedrock.closed());

        session.update(snapshot(Currency.GEMS));
        assertEquals(1, bedrock.closed(), "the open form is closed before the update is sent");

        bedrock.last().onClick().accept(bedrock.button("Player3"));
        assertEquals(1, bedrock.closed(), "an answered form is gone already");

        pages().open(player, "overview");
        assertFalse(session.isOpen());
        assertEquals(2, bedrock.closed(), "the replaced session's form does not stay in front of the new one");
    }

    @Test
    public void leavingThroughABedrockExitButtonClosesNoOtherForm() {
        PlayerMock player = server.addPlayer("Bedrock");
        bedrock.markBedrock(player.getUniqueId());
        DialogSession<String> session = pages().open(player, "overview");

        bedrock.last().onClick().accept(bedrock.button("Close"));

        assertFalse(session.isOpen());
        assertEquals(0, bedrock.closed(), "the answered form is gone already; another plugin's form must stay");
    }

    @Test
    public void closingABedrockSessionClosesItsUnansweredFormOnce() {
        PlayerMock player = server.addPlayer("Bedrock");
        bedrock.markBedrock(player.getUniqueId());
        DialogSession<String> session = pages().open(player, "overview");

        session.close();

        assertFalse(session.isOpen());
        assertEquals(1, bedrock.closed());
    }
}

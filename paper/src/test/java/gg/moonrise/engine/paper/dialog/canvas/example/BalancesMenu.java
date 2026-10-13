package gg.moonrise.engine.paper.dialog.canvas.example;

import gg.moonrise.engine.paper.dialog.canvas.DialogCanvas;
import gg.moonrise.engine.paper.dialog.canvas.DialogMenu;
import gg.moonrise.engine.paper.dialog.canvas.DialogSession;
import gg.moonrise.engine.paper.dialog.canvas.theme.DialogTheme;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.UUID;

/**
 * Reference implementation of the themed Balances leaderboard: currency tabs, a 1-3-5-7 pyramid of
 * faces, names, and balances, the viewer's own position, and a Back button.
 */
public class BalancesMenu extends DialogMenu<BalanceSnapshot> {

    private final BalanceService balances;

    public BalancesMenu(BalanceService balances, DialogTheme theme) {
        super("<aqua>Balances", theme);
        this.balances = balances;
    }

    @Override
    protected void render(DialogSession<BalanceSnapshot> session, DialogCanvas ui) {
        BalanceSnapshot state = session.data();

        ui.tabs(Currency.values(), state.currency())
                .label(Currency::displayName)
                .onSelect(currency -> session.load(() -> load(session, currency)));

        ui.centeredRows(state.rows(), (card, entry) -> {
            card.head(entry.playerId(), 16);
            card.text(Component.text(entry.playerName()));
            card.text(Component.text(entry.formattedBalance(), ui.theme().theme().accentColor()));
            card.tooltip("<gray>#" + entry.rank() + " " + entry.playerName());
        });

        ui.footerText(Component.text(state.personalSummary()));
        ui.exitButton("<gold>Back", session::close);
    }

    private BalanceSnapshot load(DialogSession<BalanceSnapshot> session, Currency currency) {
        BalanceSnapshot snapshot = balances.loadSnapshot(session.playerId(), currency);
        List<UUID> faces = snapshot.top().stream().map(BalanceSnapshot.Entry::playerId).toList();
        session.runtime().heads().preload(faces).join();
        return snapshot;
    }
}

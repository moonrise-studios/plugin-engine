package gg.moonrise.engine.paper.dialog.canvas.example;

import gg.moonrise.engine.paper.dialog.canvas.DialogLayouts;

import java.util.List;
import java.util.UUID;

/**
 * Everything the Balances screen shows for one viewer and currency.
 * @param currency the selected currency
 * @param top the leaderboard, best first
 * @param personalSummary the viewer's own rank and balance
 */
public record BalanceSnapshot(Currency currency, List<Entry> top, String personalSummary) {

    public BalanceSnapshot {
        top = List.copyOf(top);
    }

    /**
     * Groups the leaderboard into a 1-3-5-7 pyramid.
     * @return the rows
     */
    public List<List<Entry>> rows() {
        return DialogLayouts.rows(top, 1, 3, 5, 7);
    }

    /**
     * One leaderboard position.
     * @param rank the rank, starting at 1
     * @param playerId the player
     * @param playerName the player's name
     * @param formattedBalance the balance, already formatted
     */
    public record Entry(int rank, UUID playerId, String playerName, String formattedBalance) {
    }
}

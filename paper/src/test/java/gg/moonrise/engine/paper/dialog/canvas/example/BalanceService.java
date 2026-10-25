package gg.moonrise.engine.paper.dialog.canvas.example;

import java.util.UUID;

/**
 * The economy plugin's query side; may block, so it is only called from {@code DialogSession#load}.
 */
public interface BalanceService {

    BalanceSnapshot loadSnapshot(UUID viewer, Currency currency);
}

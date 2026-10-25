package gg.moonrise.engine.paper.dialog.canvas.example;

public enum Currency {
    MONEY("Money"),
    GEMS("Gems"),
    PEARLS("Pearls"),
    FARM_TOKENS("Farm Tokens"),
    DOUBLOONS("Doubloons");

    private final String displayName;

    private Currency(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

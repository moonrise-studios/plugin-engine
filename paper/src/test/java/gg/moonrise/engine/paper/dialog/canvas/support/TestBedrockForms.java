package gg.moonrise.engine.paper.dialog.canvas.support;

import gg.moonrise.engine.paper.dialog.canvas.BedrockForm;
import gg.moonrise.engine.paper.dialog.canvas.BedrockForms;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bedrock forms without Floodgate: tests choose who is a Bedrock player and answer the sent forms.
 */
public final class TestBedrockForms implements BedrockForms {

    private final Set<UUID> bedrockPlayers = new HashSet<>();
    private final List<BedrockForm> sent = new ArrayList<>();
    private int closed;

    public void markBedrock(UUID playerId) {
        bedrockPlayers.add(playerId);
    }

    @Override
    public boolean isBedrock(Player player) {
        return bedrockPlayers.contains(player.getUniqueId());
    }

    @Override
    public boolean send(Player player, BedrockForm form) {
        sent.add(form);
        return true;
    }

    @Override
    public void close(Player player) {
        closed++;
    }

    public List<BedrockForm> sent() {
        return List.copyOf(sent);
    }

    public BedrockForm last() {
        return sent.get(sent.size() - 1);
    }

    public int closed() {
        return closed;
    }

    /**
     * Index of the first button of the latest form whose text contains a value.
     * @param text the text to find
     * @return the button index
     */
    public int button(String text) {
        List<BedrockForm.Button> buttons = last().buttons();
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).text().contains(text)) return i;
        }

        throw new IllegalArgumentException("No button containing " + text + " in " + buttons);
    }
}

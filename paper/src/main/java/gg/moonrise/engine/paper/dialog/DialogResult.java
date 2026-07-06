package gg.moonrise.engine.paper.dialog;

import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Typed access to values returned by a Paper dialog response.
 */
public final class DialogResult {

    private final DialogResponseView view;
    private final Audience audience;

    DialogResult(DialogResponseView view, Audience audience) {
        this.view = view;
        this.audience = audience;
    }

    public DialogResponseView view() {
        return view;
    }

    public Audience audience() {
        return audience;
    }

    public Optional<Player> player() {
        if (audience instanceof Player player) return Optional.of(player);

        return Optional.empty();
    }

    public BinaryTagHolder payload() {
        return view.payload();
    }

    public Optional<String> text(String key) {
        return Optional.ofNullable(view.getText(key));
    }

    public String text(String key, String fallback) {
        return text(key).orElse(fallback);
    }

    public String requireText(String key) {
        return text(key).orElseThrow(() -> new IllegalArgumentException("Missing dialog text input: " + key));
    }

    public Optional<Boolean> bool(String key) {
        return Optional.ofNullable(view.getBoolean(key));
    }

    public boolean bool(String key, boolean fallback) {
        return bool(key).orElse(fallback);
    }

    public boolean requireBool(String key) {
        return bool(key).orElseThrow(() -> new IllegalArgumentException("Missing dialog boolean input: " + key));
    }

    public Optional<Float> number(String key) {
        return Optional.ofNullable(view.getFloat(key));
    }

    public float number(String key, float fallback) {
        return number(key).orElse(fallback);
    }

    public float requireNumber(String key) {
        return number(key).orElseThrow(() -> new IllegalArgumentException("Missing dialog number input: " + key));
    }
}

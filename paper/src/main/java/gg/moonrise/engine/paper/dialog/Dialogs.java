package gg.moonrise.engine.paper.dialog;

import gg.moonrise.engine.message.Message;
import gg.moonrise.engine.message.util.MiniMessageUtil;
import io.papermc.paper.registry.data.dialog.input.BooleanDialogInput;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.NumberRangeDialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

/**
 * Factory helpers for Paper dialogs.
 */
public final class Dialogs {

    private Dialogs() {
    }

    public static DialogBuilder create(Player player) {
        return new DialogBuilder(player);
    }

    public static TextDialogInput.Builder text(String key, String label) {
        return DialogInput.text(key, component(label));
    }

    public static TextDialogInput.Builder text(String key, Component label) {
        return DialogInput.text(key, label);
    }

    public static TextDialogInput.Builder text(String key, Message label) {
        return text(key, label.asComponent());
    }

    public static TextDialogInput.MultilineOptions multiline(Integer maxLines, Integer height) {
        return TextDialogInput.MultilineOptions.create(maxLines, height);
    }

    public static NumberRangeDialogInput.Builder numberRange(String key, String label, float start, float end) {
        return DialogInput.numberRange(key, component(label), start, end);
    }

    public static NumberRangeDialogInput.Builder numberRange(String key, Component label, float start, float end) {
        return DialogInput.numberRange(key, label, start, end);
    }

    public static NumberRangeDialogInput.Builder numberRange(String key, Message label, float start, float end) {
        return numberRange(key, label.asComponent(), start, end);
    }

    public static BooleanDialogInput.Builder bool(String key, String label) {
        return DialogInput.bool(key, component(label));
    }

    public static BooleanDialogInput.Builder bool(String key, Component label) {
        return DialogInput.bool(key, label);
    }

    public static BooleanDialogInput.Builder bool(String key, Message label) {
        return bool(key, label.asComponent());
    }

    public static SingleOptionDialogInput.Builder singleOption(String key, String label, List<SingleOptionDialogInput.OptionEntry> entries) {
        return DialogInput.singleOption(key, component(label), entries);
    }

    public static SingleOptionDialogInput.Builder singleOption(String key, Component label, List<SingleOptionDialogInput.OptionEntry> entries) {
        return DialogInput.singleOption(key, label, entries);
    }

    public static SingleOptionDialogInput.Builder singleOption(String key, Message label, List<SingleOptionDialogInput.OptionEntry> entries) {
        return singleOption(key, label.asComponent(), entries);
    }

    public static SingleOptionDialogInput.OptionEntry option(String id, String display) {
        return option(id, display, false);
    }

    public static SingleOptionDialogInput.OptionEntry option(String id, String display, boolean initial) {
        return SingleOptionDialogInput.OptionEntry.create(id, nullableComponent(display), initial);
    }

    public static SingleOptionDialogInput.OptionEntry option(String id, Component display, boolean initial) {
        return SingleOptionDialogInput.OptionEntry.create(id, display, initial);
    }

    static Component component(String text) {
        return MiniMessageUtil.fromText(Objects.requireNonNullElse(text, ""));
    }

    static Component nullableComponent(String text) {
        if (text == null) return null;

        return component(text);
    }
}

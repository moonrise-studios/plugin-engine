package gg.moonrise.engine.paper.dialog;

import gg.moonrise.engine.message.Message;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Fluent wrapper around dynamic Paper dialogs.
 */
public final class DialogBuilder {

    private final Player player;
    private final List<DialogBody> body = new ArrayList<>();
    private final List<DialogInput> inputs = new ArrayList<>();
    private Component title = Component.empty();
    private Component externalTitle;
    private boolean canCloseWithEscape = true;
    private boolean pause = false;
    private DialogBase.DialogAfterAction afterAction = DialogBase.DialogAfterAction.CLOSE;
    private Component submitLabel = Component.text("Submit");
    private Component submitTooltip;
    private int submitWidth = 150;
    private boolean cancelButton = true;
    private Component cancelLabel = Component.text("Cancel");
    private Component cancelTooltip;
    private int cancelWidth = 150;
    private ClickCallback.Options callbackOptions = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(ClickCallback.DEFAULT_LIFETIME)
            .build();

    DialogBuilder(Player player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    public DialogBuilder title(String title) {
        return title(Dialogs.component(title));
    }

    public DialogBuilder title(Message title) {
        return title(title.asComponent(player));
    }

    public DialogBuilder title(Component title) {
        this.title = Objects.requireNonNull(title, "title");
        return this;
    }

    public DialogBuilder externalTitle(String externalTitle) {
        return externalTitle(Dialogs.component(externalTitle));
    }

    public DialogBuilder externalTitle(Component externalTitle) {
        this.externalTitle = externalTitle;
        return this;
    }

    public DialogBuilder body(String body) {
        return body(DialogBody.plainMessage(Dialogs.component(body)));
    }

    public DialogBuilder body(Message body) {
        return body(DialogBody.plainMessage(body.asComponent(player)));
    }

    public DialogBuilder body(Component body) {
        return body(DialogBody.plainMessage(body));
    }

    public DialogBuilder body(DialogBody body) {
        this.body.add(Objects.requireNonNull(body, "body"));
        return this;
    }

    public DialogBuilder body(Collection<? extends DialogBody> body) {
        Objects.requireNonNull(body, "body");
        body.forEach(this::body);
        return this;
    }

    public DialogBuilder input(DialogInput input) {
        this.inputs.add(Objects.requireNonNull(input, "input"));
        return this;
    }

    public DialogBuilder inputs(DialogInput... inputs) {
        Objects.requireNonNull(inputs, "inputs");
        for (DialogInput input : inputs) input(input);
        return this;
    }

    public DialogBuilder inputs(Collection<? extends DialogInput> inputs) {
        Objects.requireNonNull(inputs, "inputs");
        inputs.forEach(this::input);
        return this;
    }

    public DialogBuilder canCloseWithEscape(boolean canCloseWithEscape) {
        this.canCloseWithEscape = canCloseWithEscape;
        return this;
    }

    public DialogBuilder pause(boolean pause) {
        this.pause = pause;
        return this;
    }

    public DialogBuilder afterAction(DialogBase.DialogAfterAction afterAction) {
        this.afterAction = Objects.requireNonNull(afterAction, "afterAction");
        return this;
    }

    public DialogBuilder submitButton(String label) {
        return submitButton(Dialogs.component(label), submitTooltip, submitWidth);
    }

    public DialogBuilder submitButton(Component label) {
        return submitButton(label, submitTooltip, submitWidth);
    }

    public DialogBuilder submitButton(String label, String tooltip, int width) {
        return submitButton(Dialogs.component(label), Dialogs.nullableComponent(tooltip), width);
    }

    public DialogBuilder submitButton(Component label, Component tooltip, int width) {
        this.submitLabel = Objects.requireNonNull(label, "label");
        this.submitTooltip = tooltip;
        this.submitWidth = requirePositiveWidth(width);
        return this;
    }

    public DialogBuilder cancelButton(String label) {
        this.cancelButton = true;
        this.cancelLabel = Dialogs.component(label);
        return this;
    }

    public DialogBuilder cancelButton(Component label) {
        this.cancelButton = true;
        this.cancelLabel = Objects.requireNonNull(label, "label");
        return this;
    }

    public DialogBuilder cancelButton(String label, String tooltip, int width) {
        return cancelButton(Dialogs.component(label), Dialogs.nullableComponent(tooltip), width);
    }

    public DialogBuilder cancelButton(Component label, Component tooltip, int width) {
        this.cancelButton = true;
        this.cancelLabel = Objects.requireNonNull(label, "label");
        this.cancelTooltip = tooltip;
        this.cancelWidth = requirePositiveWidth(width);
        return this;
    }

    public DialogBuilder withoutCancelButton() {
        this.cancelButton = false;
        return this;
    }

    public DialogBuilder callbackOptions(ClickCallback.Options callbackOptions) {
        this.callbackOptions = Objects.requireNonNull(callbackOptions, "callbackOptions");
        return this;
    }

    public DialogBuilder lifetime(Duration lifetime) {
        Objects.requireNonNull(lifetime, "lifetime");
        this.callbackOptions = ClickCallback.Options.builder(callbackOptions)
                .lifetime(lifetime)
                .build();
        return this;
    }

    public DialogBuilder uses(int uses) {
        this.callbackOptions = ClickCallback.Options.builder(callbackOptions)
                .uses(uses)
                .build();
        return this;
    }

    public Dialog build(DialogActionCallback callback) {
        Objects.requireNonNull(callback, "callback");

        DialogBase base = DialogBase.builder(title)
                .externalTitle(externalTitle)
                .canCloseWithEscape(canCloseWithEscape)
                .pause(pause)
                .afterAction(afterAction)
                .body(List.copyOf(body))
                .inputs(List.copyOf(inputs))
                .build();

        DialogType type = type(DialogAction.customClick(callback, callbackOptions));

        return Dialog.create(builder -> builder.empty()
                .base(base)
                .type(type));
    }

    public Dialog whenComplete(Consumer<DialogResult> handler) {
        return whenComplete(player, handler);
    }

    public Dialog whenComplete(Player target, Consumer<DialogResult> handler) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(handler, "handler");

        Dialog dialog = build((view, audience) -> handler.accept(new DialogResult(view, audience)));
        target.showDialog(dialog);
        return dialog;
    }

    public Dialog whenComplete(BiConsumer<Player, DialogResult> handler) {
        Objects.requireNonNull(handler, "handler");
        return whenComplete(player, result -> handler.accept(player, result));
    }

    public Dialog whenComplete(Player target, BiConsumer<Player, DialogResult> handler) {
        Objects.requireNonNull(handler, "handler");
        return whenComplete(target, result -> handler.accept(target, result));
    }

    private DialogType type(DialogAction submitAction) {
        ActionButton submit = ActionButton.create(submitLabel, submitTooltip, submitWidth, submitAction);
        if (!cancelButton) return DialogType.notice(submit);

        ActionButton cancel = ActionButton.create(cancelLabel, cancelTooltip, cancelWidth, null);
        return DialogType.confirmation(submit, cancel);
    }

    private static int requirePositiveWidth(int width) {
        if (width < 1) throw new IllegalArgumentException("Dialog button width must be positive");

        return width;
    }
}

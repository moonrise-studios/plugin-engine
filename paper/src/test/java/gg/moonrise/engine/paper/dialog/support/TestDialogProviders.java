package gg.moonrise.engine.paper.dialog.support;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryBuilderFactory;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.InlinedRegistryBuilderProvider;
import io.papermc.paper.registry.data.InstrumentRegistryEntry;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogInstancesProvider;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.ItemDialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.input.BooleanDialogInput;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.NumberRangeDialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.ConfirmationType;
import io.papermc.paper.registry.data.dialog.type.DialogListType;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.type.MultiActionType;
import io.papermc.paper.registry.data.dialog.type.NoticeType;
import io.papermc.paper.registry.data.dialog.type.ServerLinksType;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.set.RegistryValueSetBuilder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class TestDialogProviders implements DialogInstancesProvider, InlinedRegistryBuilderProvider {

    private static final Key CALLBACK_KEY = Key.key("test", "dialog_callback");

    @Override
    public Dialog createDialog(Consumer<RegistryBuilderFactory<Dialog, ? extends DialogRegistryEntry.Builder>> value) {
        TestDialogBuilderFactory factory = new TestDialogBuilderFactory();
        value.accept(factory);
        TestDialogBuilder builder = factory.builder();
        return new TestDialog(builder.base(), builder.type());
    }

    @Override
    public MusicInstrument createInstrument(Consumer<RegistryBuilderFactory<MusicInstrument, ? extends InstrumentRegistryEntry.Builder>> value) {
        throw new UnsupportedOperationException("Test dialog provider does not implement instruments");
    }

    @Override
    public DialogBase.Builder dialogBaseBuilder(Component title) {
        return new TestDialogBaseBuilder(title);
    }

    @Override
    public ActionButton.Builder actionButtonBuilder(Component label) {
        return new TestActionButtonBuilder(label);
    }

    @Override
    public DialogAction.CustomClickAction register(DialogActionCallback callback, ClickCallback.Options options) {
        return new TestCustomClickAction(CALLBACK_KEY, null, callback, options);
    }

    @Override
    public DialogAction.StaticAction staticAction(ClickEvent value) {
        return new TestStaticAction(value);
    }

    @Override
    public DialogAction.CommandTemplateAction commandTemplate(String template) {
        return new TestCommandTemplateAction(template);
    }

    @Override
    public DialogAction.CustomClickAction customClick(Key id, BinaryTagHolder additions) {
        return new TestCustomClickAction(id, additions, null, null);
    }

    @Override
    public ItemDialogBody.Builder itemDialogBodyBuilder(ItemStack itemStack) {
        return new TestItemDialogBodyBuilder(itemStack);
    }

    @Override
    public PlainMessageDialogBody plainMessageDialogBody(Component component) {
        return plainMessageDialogBody(component, 200);
    }

    @Override
    public PlainMessageDialogBody plainMessageDialogBody(Component component, int width) {
        return new TestPlainMessageDialogBody(component, width);
    }

    @Override
    public BooleanDialogInput.Builder booleanBuilder(String key, Component label) {
        return new TestBooleanDialogInputBuilder(key, label);
    }

    @Override
    public NumberRangeDialogInput.Builder numberRangeBuilder(String key, Component label, float start, float end) {
        return new TestNumberRangeDialogInputBuilder(key, label, start, end);
    }

    @Override
    public SingleOptionDialogInput.Builder singleOptionBuilder(String key, Component label, List<SingleOptionDialogInput.OptionEntry> entries) {
        return new TestSingleOptionDialogInputBuilder(key, label, entries);
    }

    @Override
    public SingleOptionDialogInput.OptionEntry singleOptionEntry(String id, Component display, boolean initial) {
        return new TestOptionEntry(id, display, initial);
    }

    @Override
    public TextDialogInput.Builder textBuilder(String key, Component label) {
        return new TestTextDialogInputBuilder(key, label);
    }

    @Override
    public TextDialogInput.MultilineOptions multilineOptions(Integer maxLines, Integer height) {
        return new TestMultilineOptions(maxLines, height);
    }

    @Override
    public ConfirmationType confirmation(ActionButton yesButton, ActionButton noButton) {
        return new TestConfirmationType(yesButton, noButton);
    }

    @Override
    public DialogListType.Builder dialogList(RegistrySet<Dialog> dialogs) {
        throw new UnsupportedOperationException("Test dialog provider does not implement dialog lists");
    }

    @Override
    public MultiActionType.Builder multiAction(List<ActionButton> actions) {
        return new TestMultiActionTypeBuilder(actions);
    }

    @Override
    public NoticeType notice() {
        return new TestNoticeType(null);
    }

    @Override
    public NoticeType notice(ActionButton action) {
        return new TestNoticeType(action);
    }

    @Override
    public ServerLinksType serverLinks(ActionButton exitAction, int columns, int buttonWidth) {
        return new TestServerLinksType(exitAction, columns, buttonWidth);
    }

    public record TestDialog(DialogBase base, DialogType type) implements Dialog {
        @Override
        public NamespacedKey getKey() {
            return new NamespacedKey("test", "dialog");
        }
    }

    public record TestCustomClickAction(
            Key id,
            BinaryTagHolder additions,
            DialogActionCallback callback,
            ClickCallback.Options options
    ) implements DialogAction.CustomClickAction {
    }

    private record TestStaticAction(ClickEvent value) implements DialogAction.StaticAction {
    }

    private record TestCommandTemplateAction(String template) implements DialogAction.CommandTemplateAction {
    }

    private record TestDialogBase(
            Component title,
            Component externalTitle,
            boolean canCloseWithEscape,
            boolean pause,
            DialogAfterAction afterAction,
            List<DialogBody> body,
            List<DialogInput> inputs
    ) implements DialogBase {

        private TestDialogBase {
            body = List.copyOf(body);
            inputs = List.copyOf(inputs);
        }
    }

    private record TestActionButton(Component label, Component tooltip, int width, DialogAction action) implements ActionButton {
    }

    private record TestPlainMessageDialogBody(Component contents, int width) implements PlainMessageDialogBody {
    }

    private record TestItemDialogBody(
            ItemStack item,
            PlainMessageDialogBody description,
            boolean showDecorations,
            boolean showTooltip,
            int width,
            int height
    ) implements ItemDialogBody {
    }

    private record TestBooleanDialogInput(
            String key,
            Component label,
            boolean initial,
            String onTrue,
            String onFalse
    ) implements BooleanDialogInput {
    }

    private record TestNumberRangeDialogInput(
            String key,
            int width,
            Component label,
            String labelFormat,
            float start,
            float end,
            Float initial,
            Float step
    ) implements NumberRangeDialogInput {
    }

    private record TestSingleOptionDialogInput(
            String key,
            int width,
            List<OptionEntry> entries,
            Component label,
            boolean labelVisible
    ) implements SingleOptionDialogInput {

        private TestSingleOptionDialogInput {
            entries = List.copyOf(entries);
        }
    }

    private record TestOptionEntry(String id, Component display, boolean initial) implements SingleOptionDialogInput.OptionEntry {
    }

    private record TestTextDialogInput(
            String key,
            int width,
            Component label,
            boolean labelVisible,
            String initial,
            int maxLength,
            MultilineOptions multiline
    ) implements TextDialogInput {
    }

    private record TestMultilineOptions(Integer maxLines, Integer height) implements TextDialogInput.MultilineOptions {
    }

    public record TestConfirmationType(ActionButton yesButton, ActionButton noButton) implements ConfirmationType {
    }

    public record TestNoticeType(ActionButton action) implements NoticeType {
    }

    private record TestServerLinksType(ActionButton exitAction, int columns, int buttonWidth) implements ServerLinksType {
    }

    private record TestMultiActionType(List<ActionButton> actions, ActionButton exitAction, int columns) implements MultiActionType {

        private TestMultiActionType {
            actions = List.copyOf(actions);
        }
    }

    private static final class TestDialogBuilderFactory implements RegistryBuilderFactory<Dialog, TestDialogBuilder> {

        private TestDialogBuilder builder;

        @Override
        public TestDialogBuilder empty() {
            builder = new TestDialogBuilder();
            return builder;
        }

        @Override
        public TestDialogBuilder copyFrom(TypedKey<Dialog> key) {
            return empty();
        }

        private TestDialogBuilder builder() {
            return Objects.requireNonNull(builder, "Dialog builder was not created");
        }
    }

    private static final class TestDialogBuilder implements DialogRegistryEntry.Builder {

        private DialogBase base;
        private DialogType type;

        @Override
        public DialogBase base() {
            return base;
        }

        @Override
        public DialogType type() {
            return type;
        }

        @Override
        public RegistryValueSetBuilder<Dialog, DialogRegistryEntry.Builder> registryValueSet() {
            throw new UnsupportedOperationException("Test dialog provider does not implement registry value sets");
        }

        @Override
        public DialogRegistryEntry.Builder base(DialogBase dialogBase) {
            this.base = dialogBase;
            return this;
        }

        @Override
        public DialogRegistryEntry.Builder type(DialogType dialogType) {
            this.type = dialogType;
            return this;
        }
    }

    private static final class TestDialogBaseBuilder implements DialogBase.Builder {

        private final Component title;
        private Component externalTitle;
        private boolean canCloseWithEscape = true;
        private boolean pause = false;
        private DialogBase.DialogAfterAction afterAction = DialogBase.DialogAfterAction.CLOSE;
        private List<DialogBody> body = List.of();
        private List<DialogInput> inputs = List.of();

        private TestDialogBaseBuilder(Component title) {
            this.title = title;
        }

        @Override
        public DialogBase.Builder externalTitle(Component externalTitle) {
            this.externalTitle = externalTitle;
            return this;
        }

        @Override
        public DialogBase.Builder canCloseWithEscape(boolean canCloseWithEscape) {
            this.canCloseWithEscape = canCloseWithEscape;
            return this;
        }

        @Override
        public DialogBase.Builder pause(boolean pause) {
            this.pause = pause;
            return this;
        }

        @Override
        public DialogBase.Builder afterAction(DialogBase.DialogAfterAction afterAction) {
            this.afterAction = afterAction;
            return this;
        }

        @Override
        public DialogBase.Builder body(List<? extends DialogBody> body) {
            this.body = List.copyOf(body);
            return this;
        }

        @Override
        public DialogBase.Builder inputs(List<? extends DialogInput> inputs) {
            this.inputs = List.copyOf(inputs);
            return this;
        }

        @Override
        public DialogBase build() {
            return new TestDialogBase(title, externalTitle, canCloseWithEscape, pause, afterAction, body, inputs);
        }
    }

    private static final class TestActionButtonBuilder implements ActionButton.Builder {

        private final Component label;
        private Component tooltip;
        private int width = 150;
        private DialogAction action;

        private TestActionButtonBuilder(Component label) {
            this.label = label;
        }

        @Override
        public ActionButton.Builder tooltip(Component tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        @Override
        public ActionButton.Builder width(int width) {
            this.width = width;
            return this;
        }

        @Override
        public ActionButton.Builder action(DialogAction action) {
            this.action = action;
            return this;
        }

        @Override
        public ActionButton build() {
            return new TestActionButton(label, tooltip, width, action);
        }
    }

    private static final class TestItemDialogBodyBuilder implements ItemDialogBody.Builder {

        private final ItemStack item;
        private PlainMessageDialogBody description;
        private boolean showDecorations = true;
        private boolean showTooltip = true;
        private int width = 16;
        private int height = 16;

        private TestItemDialogBodyBuilder(ItemStack item) {
            this.item = item;
        }

        @Override
        public ItemDialogBody.Builder description(PlainMessageDialogBody description) {
            this.description = description;
            return this;
        }

        @Override
        public ItemDialogBody.Builder showDecorations(boolean showDecorations) {
            this.showDecorations = showDecorations;
            return this;
        }

        @Override
        public ItemDialogBody.Builder showTooltip(boolean showTooltip) {
            this.showTooltip = showTooltip;
            return this;
        }

        @Override
        public ItemDialogBody.Builder width(int width) {
            this.width = width;
            return this;
        }

        @Override
        public ItemDialogBody.Builder height(int height) {
            this.height = height;
            return this;
        }

        @Override
        public ItemDialogBody build() {
            return new TestItemDialogBody(item, description, showDecorations, showTooltip, width, height);
        }
    }

    private static final class TestBooleanDialogInputBuilder implements BooleanDialogInput.Builder {

        private final String key;
        private final Component label;
        private boolean initial = false;
        private String onTrue = "true";
        private String onFalse = "false";

        private TestBooleanDialogInputBuilder(String key, Component label) {
            this.key = key;
            this.label = label;
        }

        @Override
        public BooleanDialogInput.Builder initial(boolean initial) {
            this.initial = initial;
            return this;
        }

        @Override
        public BooleanDialogInput.Builder onTrue(String onTrue) {
            this.onTrue = onTrue;
            return this;
        }

        @Override
        public BooleanDialogInput.Builder onFalse(String onFalse) {
            this.onFalse = onFalse;
            return this;
        }

        @Override
        public BooleanDialogInput build() {
            return new TestBooleanDialogInput(key, label, initial, onTrue, onFalse);
        }
    }

    private static final class TestNumberRangeDialogInputBuilder implements NumberRangeDialogInput.Builder {

        private final String key;
        private final Component label;
        private final float start;
        private final float end;
        private int width = 200;
        private String labelFormat = "%s: %s";
        private Float initial;
        private Float step;

        private TestNumberRangeDialogInputBuilder(String key, Component label, float start, float end) {
            this.key = key;
            this.label = label;
            this.start = start;
            this.end = end;
        }

        @Override
        public NumberRangeDialogInput.Builder width(int width) {
            this.width = width;
            return this;
        }

        @Override
        public NumberRangeDialogInput.Builder labelFormat(String labelFormat) {
            this.labelFormat = labelFormat;
            return this;
        }

        @Override
        public NumberRangeDialogInput.Builder initial(Float initial) {
            this.initial = initial;
            return this;
        }

        @Override
        public NumberRangeDialogInput.Builder step(Float step) {
            this.step = step;
            return this;
        }

        @Override
        public NumberRangeDialogInput build() {
            return new TestNumberRangeDialogInput(key, width, label, labelFormat, start, end, initial, step);
        }
    }

    private static final class TestSingleOptionDialogInputBuilder implements SingleOptionDialogInput.Builder {

        private final String key;
        private final Component label;
        private final List<SingleOptionDialogInput.OptionEntry> entries;
        private int width = 200;
        private boolean labelVisible = true;

        private TestSingleOptionDialogInputBuilder(String key, Component label, List<SingleOptionDialogInput.OptionEntry> entries) {
            this.key = key;
            this.label = label;
            this.entries = new ArrayList<>(entries);
        }

        @Override
        public SingleOptionDialogInput.Builder width(int width) {
            this.width = width;
            return this;
        }

        @Override
        public SingleOptionDialogInput.Builder labelVisible(boolean labelVisible) {
            this.labelVisible = labelVisible;
            return this;
        }

        @Override
        public SingleOptionDialogInput build() {
            return new TestSingleOptionDialogInput(key, width, entries, label, labelVisible);
        }
    }

    private static final class TestTextDialogInputBuilder implements TextDialogInput.Builder {

        private final String key;
        private final Component label;
        private int width = 200;
        private boolean labelVisible = true;
        private String initial = "";
        private int maxLength = 32;
        private TextDialogInput.MultilineOptions multiline;

        private TestTextDialogInputBuilder(String key, Component label) {
            this.key = key;
            this.label = label;
        }

        @Override
        public TextDialogInput.Builder width(int width) {
            this.width = width;
            return this;
        }

        @Override
        public TextDialogInput.Builder labelVisible(boolean labelVisible) {
            this.labelVisible = labelVisible;
            return this;
        }

        @Override
        public TextDialogInput.Builder initial(String initial) {
            this.initial = initial;
            return this;
        }

        @Override
        public TextDialogInput.Builder maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        @Override
        public TextDialogInput.Builder multiline(TextDialogInput.MultilineOptions multiline) {
            this.multiline = multiline;
            return this;
        }

        @Override
        public TextDialogInput build() {
            return new TestTextDialogInput(key, width, label, labelVisible, initial, maxLength, multiline);
        }
    }

    private static final class TestMultiActionTypeBuilder implements MultiActionType.Builder {

        private final List<ActionButton> actions;
        private ActionButton exitAction;
        private int columns = 1;

        private TestMultiActionTypeBuilder(List<ActionButton> actions) {
            this.actions = new ArrayList<>(actions);
        }

        @Override
        public MultiActionType.Builder exitAction(ActionButton exitAction) {
            this.exitAction = exitAction;
            return this;
        }

        @Override
        public MultiActionType.Builder columns(int columns) {
            this.columns = columns;
            return this;
        }

        @Override
        public MultiActionType build() {
            return new TestMultiActionType(actions, exitAction, columns);
        }
    }
}

package gg.moonrise.engine.paper.dialog;

import gg.moonrise.engine.message.util.MiniMessageUtil;
import gg.moonrise.engine.paper.dialog.support.TestDialogProviders.TestCustomClickAction;
import gg.moonrise.engine.paper.dialog.support.TestDialogProviders.TestDialog;
import gg.moonrise.engine.paper.dialog.support.TestDialogProviders.TestConfirmationType;
import gg.moonrise.engine.paper.dialog.support.TestDialogProviders.TestNoticeType;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.type.ConfirmationType;
import io.papermc.paper.registry.data.dialog.type.NoticeType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogBuilderTest extends MockBukkitTest {

    @Test
    void buildsConfirmationDialogAndCompletesWithTypedResult() {
        PlayerMock player = server.addPlayer("Eric");
        AtomicReference<DialogResult> completed = new AtomicReference<>();

        Dialog dialog = Dialogs.create(player)
                .title("<green>Profile")
                .body("Choose settings")
                .input(Dialogs.text("nickname", "Nickname").initial("Eric").maxLength(16).build())
                .input(Dialogs.numberRange("level", "Level", 0f, 100f).initial(5f).step(1f).build())
                .input(Dialogs.bool("visible", "Visible").initial(true).build())
                .submitButton("Save", "Apply changes", 120)
                .cancelButton("Discard", null, 80)
                .whenComplete(player, completed::set);

        TestDialog testDialog = assertInstanceOf(TestDialog.class, dialog);
        assertEquals("Profile", MiniMessageUtil.componentToPlainText(testDialog.base().title()));
        assertEquals(1, testDialog.base().body().size());
        assertEquals(3, testDialog.base().inputs().size());
        assertTrue(testDialog.base().canCloseWithEscape());

        ConfirmationType type = assertInstanceOf(TestConfirmationType.class, testDialog.type());
        ActionButton submitButton = type.yesButton();
        assertEquals("Save", MiniMessageUtil.componentToPlainText(submitButton.label()));
        assertEquals(120, submitButton.width());

        TestCustomClickAction action = assertInstanceOf(TestCustomClickAction.class, submitButton.action());
        action.callback().accept(response(
                Map.of("nickname", "Moon"),
                Map.of("visible", true),
                Map.of("level", 42f)
        ), player);

        DialogResult result = completed.get();
        assertEquals("Moon", result.requireText("nickname"));
        assertEquals(42f, result.requireNumber("level"));
        assertTrue(result.requireBool("visible"));
        assertSame(player, result.player().orElseThrow());
    }

    @Test
    void canBuildNoticeStyleDialogWithoutCancelButton() {
        PlayerMock player = server.addPlayer("Eric");

        Dialog dialog = Dialogs.create(player)
                .title("One Button")
                .input(Dialogs.text("value", "Value").build())
                .withoutCancelButton()
                .whenComplete(output -> {
                });

        TestDialog testDialog = assertInstanceOf(TestDialog.class, dialog);
        NoticeType type = assertInstanceOf(TestNoticeType.class, testDialog.type());
        assertEquals("Submit", MiniMessageUtil.componentToPlainText(type.action().label()));
        assertInstanceOf(TestCustomClickAction.class, type.action().action());
    }

    @Test
    void resultSupportsFallbacksAndRequiredValues() {
        DialogResult result = new DialogResult(response(
                Map.of("name", "Eric"),
                Map.of("enabled", false),
                Map.of("amount", 12.5f)
        ), Audience.empty());

        assertEquals("Eric", result.text("name", "fallback"));
        assertEquals("fallback", result.text("missing", "fallback"));
        assertFalse(result.bool("enabled", true));
        assertTrue(result.bool("missing", true));
        assertEquals(12.5f, result.number("amount", 0f));
        assertEquals(0f, result.number("missing", 0f));

        assertThrows(IllegalArgumentException.class, () -> result.requireText("missing"));
        assertThrows(IllegalArgumentException.class, () -> result.requireBool("missing"));
        assertThrows(IllegalArgumentException.class, () -> result.requireNumber("missing"));
    }

    private static TestResponseView response(Map<String, String> text, Map<String, Boolean> bool, Map<String, Float> number) {
        return new TestResponseView(text, bool, number);
    }

    private record TestResponseView(
            Map<String, String> text,
            Map<String, Boolean> bool,
            Map<String, Float> number
    ) implements DialogResponseView {

        @Override
        public BinaryTagHolder payload() {
            return BinaryTagHolder.binaryTagHolder("{}");
        }

        @Override
        public String getText(String key) {
            return text.get(key);
        }

        @Override
        public Boolean getBoolean(String key) {
            return bool.get(key);
        }

        @Override
        public Float getFloat(String key) {
            return number.get(key);
        }
    }
}

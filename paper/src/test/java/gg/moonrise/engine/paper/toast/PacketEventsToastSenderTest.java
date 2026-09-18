package gg.moonrise.engine.paper.toast;

import com.github.retrooper.packetevents.protocol.advancements.Advancement;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementDisplay;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementHolder;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementProgress;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAdvancements;
import com.github.retrooper.packetevents.PacketEvents;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import gg.moonrise.engine.paper.toast.support.TestPacketEventsAPI;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the raw packet construction. Bukkit to PacketEvents item conversion needs a
 * running PacketEvents instance, so the icon converter is stubbed out here; everything
 * else in the packet is asserted directly.
 */
public class PacketEventsToastSenderTest extends MockBukkitTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final PacketEventsToastSender sender = new PacketEventsToastSender(icon -> null);

    @BeforeEach
    public void setUpPacketEvents() {
        PacketEvents.setAPI(new TestPacketEventsAPI());
    }

    @AfterEach
    public void tearDownPacketEvents() {
        PacketEvents.setAPI(null);
    }

    @Test
    public void grantPacketRequestsAdvancementDisplay() {
        WrapperPlayServerUpdateAdvancements packet = grant(Toast.builder().title("Hello").build());

        assertTrue(packet.isShowAdvancements(), "showAdvancements must be true or 1.21.5+ clients drop the toast");
    }

    @Test
    public void grantPacketKeysProgressByTheAddedAdvancementId() {
        WrapperPlayServerUpdateAdvancements packet = grant(Toast.builder().title("Hello").build());

        List<AdvancementHolder> added = packet.getAddedAdvancements();
        assertEquals(1, added.size());

        ResourceLocation id = added.getFirst().getIdentifier();
        assertEquals(PacketEventsToastSender.TOAST_ID, id);
        assertEquals("moonrise", id.getNamespace());
        assertEquals("toast", id.getKey());

        Map<ResourceLocation, AdvancementProgress> progress = packet.getProgress();
        assertEquals(Set.of(id), progress.keySet());
        assertNotNull(progress.get(id).getCriteria().get(PacketEventsToastSender.CRITERION).getObtainedTimestamp());
    }

    @Test
    public void grantPacketCompletesTheSingleCriterion() {
        Advancement advancement = grant(Toast.builder().title("Hello").build())
                .getAddedAdvancements()
                .getFirst()
                .getAdvancement();

        assertEquals(List.of(PacketEventsToastSender.CRITERION), advancement.getCriteria());
        assertEquals(List.of(List.of(PacketEventsToastSender.CRITERION)), advancement.getRequirements());
    }

    @Test
    public void grantPacketHidesTheAdvancementButShowsTheToast() {
        AdvancementDisplay display = display(Toast.builder().title("<green>Hello").content("World").build());

        assertTrue(display.isShowToast());
        assertTrue(display.isHidden());
        assertEquals("Hello", PLAIN.serialize(display.getTitle()));
        assertEquals("", PLAIN.serialize(display.getDescription()));
    }

    @Test
    public void grantPacketRendersTheSelectedJavaLine() {
        AdvancementDisplay display = display(Toast.builder()
                .title("Hello")
                .content("World")
                .javaLine(ToastJavaLine.BOTH)
                .build());

        assertEquals("Hello World", PLAIN.serialize(display.getTitle()));
    }

    @Test
    public void grantPacketMapsEveryFrame() {
        assertEquals(AdvancementType.TASK, frame(ToastFrame.TASK));
        assertEquals(AdvancementType.GOAL, frame(ToastFrame.GOAL));
        assertEquals(AdvancementType.CHALLENGE, frame(ToastFrame.CHALLENGE));
    }

    @Test
    public void revokePacketRemovesTheToastId() {
        WrapperPlayServerUpdateAdvancements packet = sender.revokePacket();

        assertTrue(packet.getAddedAdvancements().isEmpty());
        assertTrue(packet.getProgress().isEmpty());
        assertEquals(Set.of(PacketEventsToastSender.TOAST_ID), packet.getRemovedAdvancements());
    }

    private AdvancementType frame(ToastFrame frame) {
        return display(Toast.builder().title("Hello").frame(frame).build()).getType();
    }

    private AdvancementDisplay display(Toast toast) {
        return grant(toast).getAddedAdvancements().getFirst().getAdvancement().getDisplay();
    }

    private WrapperPlayServerUpdateAdvancements grant(Toast toast) {
        PlayerMock player = server.addPlayer("Eric");
        return sender.grantPacket(player, toast);
    }
}

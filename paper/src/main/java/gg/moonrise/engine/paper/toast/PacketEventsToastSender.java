package gg.moonrise.engine.paper.toast;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.advancements.Advancement;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementDisplay;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementHolder;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementProgress;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAdvancements;
import gg.moonrise.engine.paper.scheduler.Scheduler;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Delivers toasts to Java Edition players by granting, then immediately revoking, a hidden
 * fake advancement.
 * <p>
 * Internal; not part of the supported API. This is one of only two classes that import
 * PacketEvents, and {@link Toasts} never references it unless the PacketEvents plugin is
 * enabled on this server.
 */
public final class PacketEventsToastSender implements ToastSender {

    /**
     * A single stable advancement id is reused for every toast. The client never prunes its
     * advancement progress map, so a fresh id per toast would leak client memory for the
     * whole session. Re-granting the same id still fires the toast, and a stale removal
     * arriving after a newer grant is harmless.
     */
    public static final ResourceLocation TOAST_ID = new ResourceLocation("moonrise", "toast");

    /**
     * The single criterion completed to trigger the toast.
     */
    public static final String CRITERION = "trigger";

    private static final long REMOVAL_DELAY_TICKS = 2L;
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private final Function<org.bukkit.inventory.ItemStack, com.github.retrooper.packetevents.protocol.item.ItemStack> iconConverter;

    /**
     * Creates a sender that converts icons through PacketEvents' Spigot bridge. Only construct
     * this once the PacketEvents plugin is enabled.
     */
    public PacketEventsToastSender() {
        this(SpigotConversionUtil::fromBukkitItemStack);
    }

    /**
     * Creates a sender with an explicit icon converter, so packet construction can be exercised
     * without a running PacketEvents instance.
     * @param iconConverter converts a Bukkit item into its PacketEvents equivalent
     */
    public PacketEventsToastSender(Function<org.bukkit.inventory.ItemStack, com.github.retrooper.packetevents.protocol.item.ItemStack> iconConverter) {
        this.iconConverter = Objects.requireNonNull(iconConverter, "iconConverter");
    }

    /**
     * Grants the fake advancement and schedules its removal.
     * @param player the recipient
     * @param toast the toast to deliver
     * @return {@code true} when the grant packet was sent
     */
    @Override
    public boolean send(Player player, Toast toast) {
        try {
            if (!sendPacket(player, grantPacket(player, toast))) return false;
        } catch (LinkageError | RuntimeException exception) {
            warnOnce(exception);
            return false;
        }

        // The toast is already delivered at this point, so a scheduling failure must never
        // report failure; it only means the advancement has to be revoked right away.
        try {
            scheduleRemoval(player);
        } catch (LinkageError | RuntimeException exception) {
            warnOnce(exception);
            revokeQuietly(player);
        }

        return true;
    }

    /**
     * Builds the packet that adds the fake advancement and immediately completes its criterion.
     * @param player the recipient, used to resolve the toast text
     * @param toast the toast to render
     * @return the grant packet
     */
    public WrapperPlayServerUpdateAdvancements grantPacket(Player player, Toast toast) {
        AdvancementDisplay display = new AdvancementDisplay(
                toast.javaComponent(player),
                Component.empty(),
                iconConverter.apply(toast.getIcon()),
                type(toast.getFrame()),
                null,
                true,
                true,
                0.0F,
                0.0F
        );

        Advancement advancement = new Advancement(
                null,
                display,
                List.of(CRITERION),
                List.of(List.of(CRITERION)),
                false
        );

        AdvancementProgress progress = new AdvancementProgress(Map.of(
                CRITERION,
                new AdvancementProgress.CriterionProgress(System.currentTimeMillis())
        ));

        // showAdvancements must be true: since 1.21.5 the vanilla client only queues the
        // toast when the packet asks for the advancement screen to be shown.
        return new WrapperPlayServerUpdateAdvancements(
                false,
                List.of(new AdvancementHolder(TOAST_ID, advancement)),
                Set.of(),
                Map.of(TOAST_ID, progress),
                true
        );
    }

    /**
     * Builds the packet that removes the fake advancement again.
     * @return the revoke packet
     */
    public WrapperPlayServerUpdateAdvancements revokePacket() {
        return new WrapperPlayServerUpdateAdvancements(false, List.of(), Set.of(TOAST_ID), Map.of(), true);
    }

    private void scheduleRemoval(Player player) {
        try {
            Scheduler.entity(player).runDelayed(task -> revokeQuietly(player), REMOVAL_DELAY_TICKS);
        } catch (IllegalStateException exception) {
            revokeQuietly(player);
        }
    }

    private void revokeQuietly(Player player) {
        try {
            if (!player.isOnline()) return;

            sendPacket(player, revokePacket());
        } catch (LinkageError | RuntimeException exception) {
            warnOnce(exception);
        }
    }

    private boolean sendPacket(Player player, WrapperPlayServerUpdateAdvancements packet) {
        PacketEventsAPI<?> api = PacketEvents.getAPI();
        if (api == null) return false;

        api.getPlayerManager().sendPacket(player, packet);
        return true;
    }

    private static AdvancementType type(ToastFrame frame) {
        return switch (frame) {
            case TASK -> AdvancementType.TASK;
            case GOAL -> AdvancementType.GOAL;
            case CHALLENGE -> AdvancementType.CHALLENGE;
        };
    }

    private static void warnOnce(Throwable throwable) {
        if (!WARNED.compareAndSet(false, true)) return;

        Bukkit.getLogger().log(Level.WARNING, "Java Edition toast delivery through PacketEvents failed (logged once). "
                + "The usual cause is a plugin descriptor that declares 'packetevents' without joinClasspath = true.", throwable);
    }
}

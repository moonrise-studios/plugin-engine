package gg.moonrise.engine.paper.toast.support;

import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.NettyManager;

/**
 * Minimal stand-in for a running PacketEvents instance. Packet wrappers resolve the server
 * version through {@code PacketEvents.getAPI()} in their constructor, so packet construction
 * cannot be unit tested without one.
 */
public final class TestPacketEventsAPI extends PacketEventsAPI<Object> {

    private final ServerManager serverManager = () -> ServerVersion.V_1_21_8;

    @Override
    public ServerManager getServerManager() {
        return serverManager;
    }

    @Override
    public void load() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void terminate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public Object getPlugin() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProtocolManager getProtocolManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PlayerManager getPlayerManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NettyManager getNettyManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelInjector getInjector() {
        throw new UnsupportedOperationException();
    }
}

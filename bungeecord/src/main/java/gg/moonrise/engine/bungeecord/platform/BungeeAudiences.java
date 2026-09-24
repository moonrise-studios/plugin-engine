package gg.moonrise.engine.bungeecord.platform;

import gg.moonrise.engine.bungeecord.BungeePlugin;
import gg.moonrise.engine.message.Audiences;
import gg.moonrise.moss.spring.Disableable;
import gg.moonrise.moss.spring.SpringComponent;
import net.kyori.adventure.audience.Audience;
import net.md_5.bungee.api.CommandSender;

@SpringComponent
public class BungeeAudiences extends Audiences implements Disableable {

    private final net.kyori.adventure.platform.bungeecord.BungeeAudiences adventure;

    public BungeeAudiences(BungeePlugin plugin) {
        setInstance(this);

        adventure = net.kyori.adventure.platform.bungeecord.BungeeAudiences.create(plugin);
    }

    @Override
    public Audience parse(Object o) {
        if (o instanceof CommandSender sender) {
            return adventure.sender(sender);
        }
        throw new IllegalStateException("Cannot parse " + o.getClass().getName());
    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            this.adventure.close();
        }
    }
    
}

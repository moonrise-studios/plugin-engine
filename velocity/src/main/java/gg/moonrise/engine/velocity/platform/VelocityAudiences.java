package gg.moonrise.engine.velocity.platform;

import gg.moonrise.engine.message.Audiences;
import gg.moonrise.moss.spring.SpringComponent;
import net.kyori.adventure.audience.Audience;

@SpringComponent
public class VelocityAudiences extends Audiences {

    public VelocityAudiences() {
        setInstance(this);
    }

    @Override
    public Audience parse(Object object) {
        if (object instanceof Audience audience) {
            return audience;
        }
        throw new IllegalStateException("Cannot parse " + object.getClass().getName());
    }
}

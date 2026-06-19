package gg.moonrise.engine.paper.platform;

import gg.moonrise.engine.message.Audiences;
import gg.moonrise.moss.spring.SpringComponent;
import net.kyori.adventure.audience.Audience;

@SpringComponent
public class PaperAudiences extends Audiences {

    public PaperAudiences() {
        setInstance(this);
    }

    @Override
    public Audience parse(Object o) {
        if (o instanceof Audience audience) {
            return audience;
        }
        throw new IllegalStateException("Cannot parse " + o.getClass().getSimpleName());
    }

}

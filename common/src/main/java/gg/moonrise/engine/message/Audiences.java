package gg.moonrise.engine.message;

import lombok.AccessLevel;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;

public abstract class Audiences {

    @Setter(AccessLevel.PROTECTED)
    private static Audiences instance;

    public abstract Audience parse(Object o);

    public static Audience audience(Object o) {
        return instance.parse(o);
    }

}

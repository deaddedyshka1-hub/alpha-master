package system.alpha.api.event.events.player.move;

import lombok.Getter;
import system.alpha.api.event.events.Event;

public class CollisionEvent extends Event<CollisionEvent> {
    @Getter private static final CollisionEvent instance = new CollisionEvent();
}

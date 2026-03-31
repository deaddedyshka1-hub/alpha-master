package system.alpha.api.event.events.player.move;

import lombok.Getter;
import system.alpha.api.event.events.Event;

public class TravelEvent extends Event<TravelEvent> {
    @Getter private static final TravelEvent instance = new TravelEvent();
}

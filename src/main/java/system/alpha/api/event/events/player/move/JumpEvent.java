package system.alpha.api.event.events.player.move;

import lombok.Getter;
import system.alpha.api.event.events.Event;

public class JumpEvent extends Event<JumpEvent> {
    @Getter private static final JumpEvent instance = new JumpEvent();
}

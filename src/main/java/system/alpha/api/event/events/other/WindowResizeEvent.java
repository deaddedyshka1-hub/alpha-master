package system.alpha.api.event.events.other;

import lombok.Getter;
import system.alpha.api.event.events.Event;

public class WindowResizeEvent extends Event<WindowResizeEvent> {
    @Getter private static final WindowResizeEvent instance = new WindowResizeEvent();
}

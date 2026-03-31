package system.alpha.api.event.events.client;

import lombok.Getter;
import system.alpha.api.event.events.Event;

public class GameLoopEvent extends Event<GameLoopEvent> {
    @Getter private static final GameLoopEvent instance = new GameLoopEvent();
}

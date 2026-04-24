package system.alpha.api.event.events.player.other;

import lombok.Getter;
import system.alpha.api.event.events.Event;

@Getter
public class PlayerLeaveEvent extends Event<PlayerLeaveEvent> {
    private final String playerName;

    public PlayerLeaveEvent(String playerName) {
        this.playerName = playerName;
    }
}

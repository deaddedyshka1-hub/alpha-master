package system.alpha.api.event.events.player.move;

import lombok.Getter;
import system.alpha.api.event.events.Event;

public class UpdateWalkingPlayerEvent extends Event<UpdateWalkingPlayerEvent.UpdateWalkingPlayerEventData> {
    @Getter
    private static final UpdateWalkingPlayerEvent instance = new UpdateWalkingPlayerEvent();

    @Override
    public boolean call(UpdateWalkingPlayerEventData any) {
        super.call(any);
        return true;
    }

    public static class UpdateWalkingPlayerEventData {
        private boolean cancelled = false;

        public boolean isCancelled() {
            return cancelled;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }
}
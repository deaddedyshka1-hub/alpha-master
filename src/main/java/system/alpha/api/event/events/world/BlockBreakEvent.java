package system.alpha.api.event.events.world;

import lombok.Getter;
import system.alpha.api.event.events.Event;

public class BlockBreakEvent extends Event<BlockBreakEvent> {

    @Getter
    private static final BlockBreakEvent instance = new BlockBreakEvent();
}
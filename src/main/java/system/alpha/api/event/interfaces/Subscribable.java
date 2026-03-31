package system.alpha.api.event.interfaces;

import system.alpha.api.event.EventListener;

public interface Subscribable<L, T> {
    EventListener subscribe(L listener);
    void unsubscribe(L listener);
}

package system.alpha.api.event.interfaces;

import system.alpha.api.event.Listener;

public interface Cacheable<T> {
    Listener<T>[] getCache();
}
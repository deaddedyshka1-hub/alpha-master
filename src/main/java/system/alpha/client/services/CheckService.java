package system.alpha.client.services;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class CheckService {
    private static CheckService instance;
    private final ConcurrentHashMap<String, CheckSession> activeChecks = new ConcurrentHashMap<>();
    private long defaultTime = 300000;
    private long additionalTimeAmount = 30000;

    public static CheckService getInstance() {
        if (instance == null) {
            instance = new CheckService();
        }
        return instance;
    }

    public void setDefaultTime(long timeMillis) {
        this.defaultTime = timeMillis;
    }

    public void setAdditionalTimeAmount(long timeMillis) {
        this.additionalTimeAmount = timeMillis;
    }

    public long getDefaultTime() {
        return defaultTime;
    }

    public long getAdditionalTimeAmount() {
        return additionalTimeAmount;
    }

    public void startCheck(String playerName) {
        activeChecks.put(playerName.toLowerCase(), new CheckSession(playerName, defaultTime));
    }

    public void stopCheck(String playerName) {
        activeChecks.remove(playerName.toLowerCase());
    }

    public void addTime(String playerName, long additionalTime) {
        CheckSession session = activeChecks.get(playerName.toLowerCase());
        if (session != null) {
            session.addTime(additionalTime);
        }
    }

    public void setTime(String playerName, long newTime) {
        CheckSession session = activeChecks.get(playerName.toLowerCase());
        if (session != null) {
            session.setRemainingTime(newTime);
        }
    }

    public CheckSession getCheck(String playerName) {
        return activeChecks.get(playerName.toLowerCase());
    }

    public boolean isBeingChecked(String playerName) {
        return activeChecks.containsKey(playerName.toLowerCase());
    }

    public Collection<CheckSession> getActiveChecks() {
        return activeChecks.values();
    }

    public void update() {
        activeChecks.values().removeIf(session -> {
            session.update();
            return session.isExpired();
        });
    }

    public static class CheckSession {
        private final String playerName;
        private long remainingTime;
        private long lastUpdate;
        private boolean expired = false;

        public CheckSession(String playerName, long initialTime) {
            this.playerName = playerName;
            this.remainingTime = initialTime;
            this.lastUpdate = System.currentTimeMillis();
        }

        public void update() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastUpdate;
            remainingTime -= elapsed;
            lastUpdate = now;

            if (remainingTime <= 0 && !expired) {
                expired = true;
            }
        }

        public void addTime(long additionalTime) {
            this.remainingTime += additionalTime;
        }

        public void setRemainingTime(long time) {
            this.remainingTime = time;
            this.lastUpdate = System.currentTimeMillis();
        }

        public String getPlayerName() {
            return playerName;
        }

        public long getRemainingTime() {
            return Math.max(0, remainingTime);
        }

        public boolean isExpired() {
            return expired;
        }

        public String getFormattedTime() {
            long seconds = getRemainingTime() / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
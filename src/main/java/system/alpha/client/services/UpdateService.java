package system.alpha.client.services;

import lombok.Getter;
import net.minecraft.client.gui.screen.TitleScreen;
import system.alpha.api.system.backend.ClientInfo;
import system.alpha.api.system.interfaces.QuickImports;
import system.alpha.client.ui.custom.CustomScreen;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Getter
public class UpdateService implements QuickImports {
    private static UpdateService instance;
    private String latestVersion;
    private boolean updateAvailable = false;
    private String downloadUrl = "https://github.com/deaddedyshka1-hub/alpha-master/releases";
    private boolean checked = false;

    public static UpdateService getInstance() {
        if (instance == null) {
            instance = new UpdateService();
        }
        return instance;
    }

    public void checkForUpdates() {
        if (checked) return;
        checked = true;

        new Thread(() -> {
            try {
                String currentVersion = ClientInfo.VERSION;
                System.out.println("[UpdateService] Current version: " + currentVersion);

                URL url = new URL("https://pastebin.com/raw/HaD8Adtj");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = reader.readLine();
                reader.close();

                if (line == null) {
                    System.out.println("[UpdateService] Empty response from Pastebin");
                    return;
                }

                System.out.println("[UpdateService] Raw response: " + line);

                String versionStr = line.trim();

                if (versionStr.contains("=")) {
                    String[] parts = versionStr.split("=");
                    if (parts.length > 1) {
                        versionStr = parts[1];
                    } else {
                        versionStr = parts[0];
                    }
                }

                versionStr = versionStr.trim();
                System.out.println("[UpdateService] Parsed version: " + versionStr);

                latestVersion = versionStr;

                if (isVersionValid(latestVersion)) {
                    updateAvailable = compareVersions(latestVersion, currentVersion) > 0;
                } else {
                    System.out.println("[UpdateService] Invalid version format: " + latestVersion);
                    updateAvailable = false;
                }

                System.out.println("[UpdateService] Update available: " + updateAvailable);

                if (updateAvailable) {
                    waitForTitleScreenAndShow();
                }
            } catch (Exception e) {
                System.err.println("[UpdateService] Failed to check for updates: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void waitForTitleScreenAndShow() {
        new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    Thread.sleep(500);
                    if (mc.currentScreen instanceof TitleScreen) {
                        break;
                    }
                }

                Thread.sleep(1000);

                mc.executeSync(() -> {
                    System.out.println("[UpdateService] Opening update screen");
                    mc.setScreen(new CustomScreen());
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private boolean isVersionValid(String version) {
        if (version == null || version.isEmpty()) return false;
        return version.matches("\\d+(\\.\\d+)*");
    }

    private int compareVersions(String version1, String version2) {
        try {
            if (!isVersionValid(version1) || !isVersionValid(version2)) {
                return 0;
            }

            String[] v1Parts = version1.split("\\.");
            String[] v2Parts = version2.split("\\.");

            int length = Math.max(v1Parts.length, v2Parts.length);
            for (int i = 0; i < length; i++) {
                int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
                int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
                if (v1Part != v2Part) {
                    return Integer.compare(v1Part, v2Part);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("[UpdateService] Version parse error: " + e.getMessage());
            return 0;
        }
        return 0;
    }
}
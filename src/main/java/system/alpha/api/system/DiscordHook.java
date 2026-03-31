package system.alpha.api.system;

import eu.donyka.discord.RPCHandler;
import eu.donyka.discord.discord.RichPresence;
import eu.donyka.discord.discord.RichPresenceBuilder;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import system.alpha.api.system.backend.ClientInfo;
import system.alpha.api.system.interfaces.QuickImports;

@UtilityClass
public class DiscordHook implements QuickImports {
    @SneakyThrows
    public void startRPC() {

        RPCHandler.setOnReady(user -> {
            RichPresence presence = RichPresence.builder()
                    .details("Version: " + ClientInfo.VERSION)
                    .largeImageKey("https://media1.tenor.com/m/lhAa2WiLwfkAAAAC/alpha.gif")
                    .largeImageText(user.getUsername())
                    .button(RichPresenceBuilder.RPCButton.of("Скачать", "https://t.me/alphavisual"))
                    .button(RichPresenceBuilder.RPCButton.of("Подписаться", "https://www.youtube.com/@mnchst1"))
                    .build();

            RPCHandler.updatePresence(presence);
        });

        RPCHandler.setOnDisconnected(error -> {
            System.out.println("RPC Disconnected: " + error);
        });

        RPCHandler.setOnErrored(error -> {
            System.out.println("RPC Errored: " +  error);
        });

        RPCHandler.startup("1275439142825758845", false);
    }

    public void stopRPC() {
        RPCHandler.shutdown();
    }
}

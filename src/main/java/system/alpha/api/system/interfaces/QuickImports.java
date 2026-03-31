package system.alpha.api.system.interfaces;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.packet.Packet;
import system.alpha.api.utils.other.NetworkUtil;
import system.alpha.api.utils.other.TextUtil;

public interface QuickImports {
    MinecraftClient mc = MinecraftClient.getInstance();

    default void print(String message) {
        TextUtil.sendMessage(message);
    }

    default void sendPacket(Packet<?> packet) {
        NetworkUtil.sendPacket(packet);
    }
}

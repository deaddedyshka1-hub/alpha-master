package system.alpha.client.features.modules.player;

import lombok.Getter;
import net.minecraft.util.Identifier;
import system.alpha.api.system.files.FileUtil;

import java.util.Arrays;

@Getter
public enum CustomModelType {
    CRAZY_RABBIT("Безумный кролик", "images/models/rabbit.png", ModelKey.RABBIT),
    WHITE_DEMON("Белый демон", "images/models/whitedemon.png", ModelKey.DEMON),
    RED_DEMON("Красный демон", "images/models/reddemon.png", ModelKey.DEMON),
    FREDDY_BEAR("Фредди медведь", "images/models/freddy.png", ModelKey.FREDDY),
    AMOGUS("Амогус", "images/models/amogus.png", ModelKey.AMOGUS);

    private final String displayName;
    private final Identifier texture;
    private final ModelKey modelKey;

    CustomModelType(String displayName, String texturePath, ModelKey modelKey) {
        this.displayName = displayName;
        this.texture = Identifier.of("alphavisuals", texturePath);
        this.modelKey = modelKey;
    }

    public static CustomModelType fromDisplay(String name) {
        if (name == null) {
            return null;
        }
        return Arrays.stream(CustomModelType.values())
                .filter(type -> type.displayName.equalsIgnoreCase(name) || type.matchesLegacyName(name))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesLegacyName(String name) {
        return switch (this) {
            case CRAZY_RABBIT -> "Crazy Rabbit".equalsIgnoreCase(name);
            case WHITE_DEMON -> "White Demon".equalsIgnoreCase(name);
            case RED_DEMON -> "Red Demon".equalsIgnoreCase(name);
            case FREDDY_BEAR -> "Freddy Bear".equalsIgnoreCase(name);
            case AMOGUS -> "Amogus".equalsIgnoreCase(name);
        };
    }

    public static String[] names() {
        return Arrays.stream(CustomModelType.values())
                .map(CustomModelType::getDisplayName)
                .toArray(String[]::new);
    }

    public enum ModelKey {
        RABBIT,
        DEMON,
        FREDDY,
        AMOGUS
    }
}
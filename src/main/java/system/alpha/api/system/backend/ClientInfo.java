package system.alpha.api.system.backend;

import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

@UtilityClass
public class ClientInfo {
    public final String NAME = "AlphaVisuals";
    public final String VERSION = FabricLoader.getInstance().getModContainer(NAME.toLowerCase()).get().getMetadata().getVersion().getFriendlyString();

    public final String CONFIG_PATH_OTHER = new File(System.getProperty("user.dir"), NAME + "/other").getAbsolutePath();
    public final String CONFIG_PATH_THEMES = new File(System.getProperty("user.dir"), NAME + "/themes").getAbsolutePath();
    public final String CONFIG_PATH_MAIN = new File(System.getProperty("user.dir"), NAME + "/main").getAbsolutePath();
}

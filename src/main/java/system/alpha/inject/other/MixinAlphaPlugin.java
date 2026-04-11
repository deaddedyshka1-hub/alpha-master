package system.alpha.inject.other;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class MixinAlphaPlugin
        implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE = "system.alpha.inject.";
    private static final Set<String> LUNAR_DISABLED = Set.of("client.render.LivingEntityRendererMixin", "client.MouseMixin");
    private static final Set<String> LABY_DISABLED = Set.of("client.MouseMixin");
    private static boolean lunarClient;
    private static boolean labyClient;

    public void onLoad(String mixinPackage) {
        lunarClient = MixinAlphaPlugin.isClassPresent("com.moonsworth.lunar.genesis.Genesis") || MixinAlphaPlugin.isClassPresent("com.moonsworth.lunar.ichor.Ichor") || MixinAlphaPlugin.isClassPresent("com.moonsworth.lunar.client.LunarClient") || MixinAlphaPlugin.classPathLooksLikeLunar();
        labyClient = MixinAlphaPlugin.isClassPresent("net.labymod.api.Laby") || MixinAlphaPlugin.isClassPresent("net.labymod.main.LabyMod") || MixinAlphaPlugin.classPathLooksLikeLaby();
    }

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simpleName;
        String string = simpleName = mixinClassName.startsWith(MIXIN_PACKAGE) ? mixinClassName.substring(MIXIN_PACKAGE.length()) : mixinClassName;
        if (lunarClient && LUNAR_DISABLED.contains(simpleName)) {
            return false;
        }
        return !labyClient || !LABY_DISABLED.contains(simpleName);
    }

    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    public List<String> getMixins() {
        return null;
    }

    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isClassPresent(String name) {
        try {
            Class.forName(name, false, MixinAlphaPlugin.class.getClassLoader());
            return true;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean classPathLooksLikeLunar() {
        try {
            String classPath = System.getProperty("java.class.path", "").toLowerCase();
            return classPath.contains("genesis-0.1.0-snapshot-all.jar") || classPath.contains("ichor") || classPath.contains("lunar");
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean classPathLooksLikeLaby() {
        try {
            String classPath = System.getProperty("java.class.path", "").toLowerCase();
            return classPath.contains("labymod") || classPath.contains("labyfabric") || classPath.contains("labymod-neo");
        }
        catch (Throwable ignored) {
            return false;
        }
    }
}

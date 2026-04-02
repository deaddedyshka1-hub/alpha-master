package system.alpha.inject.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import system.alpha.client.features.modules.player.CapeModule;

@Mixin(value = PlayerListEntry.class, priority = 2000)
public class MixinCapeFeatureRenderer {

    private static final Identifier CUSTOM_CAPE = Identifier.of("alphavisuals", "images/cape/cape.png");

    @Shadow @Final private GameProfile profile;

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    public void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        CapeModule module = CapeModule.getInstance();

        if (module != null) {
            SkinTextures original = cir.getReturnValue();
            cir.setReturnValue(new SkinTextures(
                    original.texture(),
                    original.textureUrl(),
                    CUSTOM_CAPE,
                    original.elytraTexture(),
                    original.model(),
                    original.secure()
            ));
        }
    }
}

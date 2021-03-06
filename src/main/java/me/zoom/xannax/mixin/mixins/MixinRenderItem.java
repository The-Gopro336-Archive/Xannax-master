package me.zoom.xannax.mixin.mixins;

import java.awt.Color;

import me.zoom.xannax.util.ColorUtil;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.renderer.GlStateManager;
import me.zoom.xannax.module.modules.render.EnchantColor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import me.zoom.xannax.module.ModuleManager;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.RenderItem;
import org.spongepowered.asm.mixin.Mixin;


@Mixin({ RenderItem.class })
public class MixinRenderItem
{
    @ModifyArg(method = { "renderEffect" }, at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/RenderItem.renderModel(Lnet/minecraft/client/renderer/block/model/IBakedModel;I)V"), index = 1)
    private int renderEffect(final int n) {
        if (!ModuleManager.isModuleEnabled("EnchantColor")) {
            return -8372020;
        }
        return ColorUtil.color(EnchantColor.red.getValue(), EnchantColor.green.getValue(), EnchantColor.blue.getValue());
    }
}

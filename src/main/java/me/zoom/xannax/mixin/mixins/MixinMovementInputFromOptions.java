package me.zoom.xannax.mixin.mixins;

import me.zoom.xannax.module.ModuleManager;
import me.zoom.xannax.clickgui.ClickGUI;
import me.zoom.xannax.util.Wrapper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.*;

@Mixin(value = MovementInputFromOptions.class, priority = 10000)
public abstract class MixinMovementInputFromOptions extends MovementInput {
    @Mutable
    @Shadow @Final private final GameSettings gameSettings;

    protected MixinMovementInputFromOptions(GameSettings gameSettings) {
        this.gameSettings = gameSettings;
    }

    /**
     * @author b
     */
    @Overwrite
    public void updatePlayerMoveState() {
        this.moveStrafe = 0.0F;
        this.moveForward = 0.0F;
        if (isKeyHeld(this.gameSettings.keyBindForward)) {
            ++this.moveForward;
            this.forwardKeyDown = true;
        } else {
            this.forwardKeyDown = false;
        }
        if (isKeyHeld(this.gameSettings.keyBindBack)) {
            --this.moveForward;
            this.backKeyDown = true;
        } else {
            this.backKeyDown = false;
        }
        if (isKeyHeld(this.gameSettings.keyBindLeft)) {
            ++this.moveStrafe;
            this.leftKeyDown = true;
        } else {
            this.leftKeyDown = false;
        }
        if (isKeyHeld(this.gameSettings.keyBindRight)) {
            --this.moveStrafe;
            this.rightKeyDown = true;
        } else {
            this.rightKeyDown = false;
        }
        this.jump = isKeyHeld(this.gameSettings.keyBindJump);
        this.sneak = isKeyHeld(this.gameSettings.keyBindSneak);
        if (this.sneak) {
            this.moveStrafe = (float) ((double) this.moveStrafe * 0.3D);
            this.moveForward = (float) ((double) this.moveForward * 0.3D);
        }
    }

    public boolean isKeyHeld(KeyBinding keyBinding) {
        if (ModuleManager.isModuleEnabled("GuiMove") && Wrapper.getMinecraft().currentScreen != null) {
            if (Wrapper.getMinecraft().currentScreen instanceof InventoryEffectRenderer) {
                return Keyboard.isKeyDown(keyBinding.getKeyCode());
            } else if (Wrapper.getMinecraft().world.isRemote && Wrapper.getMinecraft().currentScreen instanceof GuiIngameMenu) {
                return Keyboard.isKeyDown(keyBinding.getKeyCode());
            } else if (Wrapper.getMinecraft().currentScreen instanceof ClickGUI) {
                return Keyboard.isKeyDown(keyBinding.getKeyCode());
            } //sex
        }
        return keyBinding.isKeyDown();
    }
}

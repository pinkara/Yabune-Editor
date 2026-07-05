package com.pinkara.ye.gui;

import com.pinkara.ye.YEKeyHandlerClient;
import com.pinkara.ye.network.PacketYEKey;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public abstract class NonBlockingScreen extends Screen {
    protected NonBlockingScreen(Component title) {
        super(title);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // Keep all key bindings synchronised with the physical keyboard state while this
        // screen is open. Vanilla only updates KeyMapping states when no screen is shown,
        // so without this the player cannot move while the editor menu is open.
        KeyMapping.setAll();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (handleMovementKey(keyCode, true)) {
            return true;
        }
        if (handleHotbarKey(keyCode)) {
            return true;
        }
        if (handleNumpadTransform(keyCode)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_E) {
            // Close this screen and open the inventory immediately so E works while the
            // editor menu is visible.
            Minecraft mc = Minecraft.getInstance();
            this.onClose();
            if (mc.player != null) {
                mc.setScreen(new InventoryScreen(mc.player));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (handleMovementKey(keyCode, false)) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private boolean handleMovementKey(int keyCode, boolean pressed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        KeyMapping key = null;
        if (keyCode == mc.options.keyUp.getKey().getValue()) key = mc.options.keyUp;
        else if (keyCode == mc.options.keyLeft.getKey().getValue()) key = mc.options.keyLeft;
        else if (keyCode == mc.options.keyDown.getKey().getValue()) key = mc.options.keyDown;
        else if (keyCode == mc.options.keyRight.getKey().getValue()) key = mc.options.keyRight;
        else if (keyCode == mc.options.keyJump.getKey().getValue()) key = mc.options.keyJump;
        else if (keyCode == mc.options.keyShift.getKey().getValue()) key = mc.options.keyShift;
        else if (keyCode == mc.options.keySprint.getKey().getValue()) key = mc.options.keySprint;
        if (key != null) {
            key.setDown(pressed);
            return true;
        }
        return false;
    }

    private boolean handleHotbarKey(int keyCode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        for (int i = 0; i < 9; ++i) {
            if (keyCode == mc.options.keyHotbarSlots[i].getKey().getValue()) {
                mc.player.getInventory().selected = i;
                return true;
            }
        }
        return false;
    }

    private boolean handleNumpadTransform(int keyCode) {
        // Don't intercept numpad digits when the user is typing in a text field.
        if (this.getFocused() instanceof EditBox) {
            return false;
        }
        Byte key = switch (keyCode) {
            case GLFW.GLFW_KEY_KP_8 -> YEKeyHandlerClient.KEY_NP_RotX;
            case GLFW.GLFW_KEY_KP_2 -> YEKeyHandlerClient.KEY_NP_RotX_Minus;
            case GLFW.GLFW_KEY_KP_4 -> YEKeyHandlerClient.KEY_NP_RotY;
            case GLFW.GLFW_KEY_KP_6 -> YEKeyHandlerClient.KEY_NP_RotY_Minus;
            case GLFW.GLFW_KEY_KP_7 -> YEKeyHandlerClient.KEY_NP_RotZ;
            case GLFW.GLFW_KEY_KP_9 -> YEKeyHandlerClient.KEY_NP_RotZ_Minus;
            case GLFW.GLFW_KEY_KP_1 -> YEKeyHandlerClient.KEY_NP_MirrorX;
            case GLFW.GLFW_KEY_KP_5 -> YEKeyHandlerClient.KEY_NP_MirrorY;
            case GLFW.GLFW_KEY_KP_3 -> YEKeyHandlerClient.KEY_NP_MirrorZ;
            default -> null;
        };
        if (key != null) {
            PacketDistributor.sendToServer(new PacketYEKey(key));
            return true;
        }
        return false;
    }
}

package com.pinkara.ye;

import com.mojang.blaze3d.platform.InputConstants;
import com.pinkara.ye.client.EntityEditorRenderer;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = YE.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class YEKeyHandlerClient {
    // Match original YE key IDs
    public static final byte KEY_EditMode = 0;
    public static final byte KEY_Clear = 6;
    public static final byte KEY_EditMenu = 7;
    public static final byte KEY_Undo = 8;
    public static final byte KEY_Delete = 9;
    public static final byte KEY_Cut = 10;
    public static final byte KEY_Copy = 11;
    public static final byte KEY_Paste = 12;
    public static final byte KEY_Fill = 13;

    // Numpad direct transform keys
    public static final byte KEY_NP_RotX = 20;
    public static final byte KEY_NP_RotX_Minus = 21;
    public static final byte KEY_NP_RotY = 22;
    public static final byte KEY_NP_RotY_Minus = 23;
    public static final byte KEY_NP_RotZ = 24;
    public static final byte KEY_NP_RotZ_Minus = 25;
    public static final byte KEY_NP_MirrorX = 26;
    public static final byte KEY_NP_MirrorY = 27;
    public static final byte KEY_NP_MirrorZ = 28;

    // Clone box adjustment keys (in-game only)
    public static final byte KEY_CloneXMinus = 30;
    public static final byte KEY_CloneXPlus = 31;
    public static final byte KEY_CloneYMinus = 32;
    public static final byte KEY_CloneYPlus = 33;
    public static final byte KEY_CloneZMinus = 34;
    public static final byte KEY_CloneZPlus = 35;
    public static final byte KEY_CloneCountMinus = 36;
    public static final byte KEY_CloneCountPlus = 37;

    public static KeyMapping KEY_EDIT_MODE;
    public static KeyMapping KEY_EDIT_MENU;
    public static KeyMapping KEY_UNDO;
    public static KeyMapping KEY_CLEAR;
    public static KeyMapping KEY_DELETE;
    public static KeyMapping KEY_CUT;
    public static KeyMapping KEY_COPY;
    public static KeyMapping KEY_PASTE;
    public static KeyMapping KEY_FILL;

    public static KeyMapping KEY_NP_ROT_X;
    public static KeyMapping KEY_NP_ROT_X_MINUS;
    public static KeyMapping KEY_NP_ROT_Y;
    public static KeyMapping KEY_NP_ROT_Y_MINUS;
    public static KeyMapping KEY_NP_ROT_Z;
    public static KeyMapping KEY_NP_ROT_Z_MINUS;
    public static KeyMapping KEY_NP_MIRROR_X;
    public static KeyMapping KEY_NP_MIRROR_Y;
    public static KeyMapping KEY_NP_MIRROR_Z;

    public static KeyMapping KEY_CLONE_X_MINUS;
    public static KeyMapping KEY_CLONE_X_PLUS;
    public static KeyMapping KEY_CLONE_Y_MINUS;
    public static KeyMapping KEY_CLONE_Y_PLUS;
    public static KeyMapping KEY_CLONE_Z_MINUS;
    public static KeyMapping KEY_CLONE_Z_PLUS;
    public static KeyMapping KEY_CLONE_COUNT_MINUS;
    public static KeyMapping KEY_CLONE_COUNT_PLUS;

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(YE.ENTITY_EDITOR.get(), EntityEditorRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        KEY_EDIT_MODE = new KeyMapping("key.ye.edit_mode", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.ye");
        KEY_EDIT_MENU = new KeyMapping("key.ye.edit_menu", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.ye");
        KEY_UNDO = new KeyMapping("key.ye.undo", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.ye");
        KEY_CLEAR = new KeyMapping("key.ye.clear", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.ye");
        KEY_DELETE = new KeyMapping("key.ye.delete", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DELETE, "key.categories.ye");
        KEY_CUT = new KeyMapping("key.ye.cut", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.ye");
        KEY_COPY = new KeyMapping("key.ye.copy", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.ye");
        KEY_PASTE = new KeyMapping("key.ye.paste", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.ye");
        KEY_FILL = new KeyMapping("key.ye.fill", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.ye");

        // Numpad transform bindings
        KEY_NP_ROT_X = new KeyMapping("key.ye.np_rot_x", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_8, "key.categories.ye");
        KEY_NP_ROT_X_MINUS = new KeyMapping("key.ye.np_rot_x_minus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_2, "key.categories.ye");
        KEY_NP_ROT_Y = new KeyMapping("key.ye.np_rot_y", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_4, "key.categories.ye");
        KEY_NP_ROT_Y_MINUS = new KeyMapping("key.ye.np_rot_y_minus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_6, "key.categories.ye");
        KEY_NP_ROT_Z = new KeyMapping("key.ye.np_rot_z", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_7, "key.categories.ye");
        KEY_NP_ROT_Z_MINUS = new KeyMapping("key.ye.np_rot_z_minus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_9, "key.categories.ye");
        KEY_NP_MIRROR_X = new KeyMapping("key.ye.np_mirror_x", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_1, "key.categories.ye");
        KEY_NP_MIRROR_Y = new KeyMapping("key.ye.np_mirror_y", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_5, "key.categories.ye");
        KEY_NP_MIRROR_Z = new KeyMapping("key.ye.np_mirror_z", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_3, "key.categories.ye");

        KEY_CLONE_X_MINUS = new KeyMapping("key.ye.clone_x_minus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, "key.categories.ye");
        KEY_CLONE_X_PLUS = new KeyMapping("key.ye.clone_x_plus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, "key.categories.ye");
        KEY_CLONE_Y_MINUS = new KeyMapping("key.ye.clone_y_minus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_DOWN, "key.categories.ye");
        KEY_CLONE_Y_PLUS = new KeyMapping("key.ye.clone_y_plus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_UP, "key.categories.ye");
        KEY_CLONE_Z_MINUS = new KeyMapping("key.ye.clone_z_minus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP, "key.categories.ye");
        KEY_CLONE_Z_PLUS = new KeyMapping("key.ye.clone_z_plus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, "key.categories.ye");
        KEY_CLONE_COUNT_MINUS = new KeyMapping("key.ye.clone_count_minus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_HOME, "key.categories.ye");
        KEY_CLONE_COUNT_PLUS = new KeyMapping("key.ye.clone_count_plus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_END, "key.categories.ye");

        event.register(KEY_EDIT_MODE);
        event.register(KEY_EDIT_MENU);
        event.register(KEY_UNDO);
        event.register(KEY_CLEAR);
        event.register(KEY_DELETE);
        event.register(KEY_CUT);
        event.register(KEY_COPY);
        event.register(KEY_PASTE);
        event.register(KEY_FILL);

        event.register(KEY_NP_ROT_X);
        event.register(KEY_NP_ROT_X_MINUS);
        event.register(KEY_NP_ROT_Y);
        event.register(KEY_NP_ROT_Y_MINUS);
        event.register(KEY_NP_ROT_Z);
        event.register(KEY_NP_ROT_Z_MINUS);
        event.register(KEY_NP_MIRROR_X);
        event.register(KEY_NP_MIRROR_Y);
        event.register(KEY_NP_MIRROR_Z);

        event.register(KEY_CLONE_X_MINUS);
        event.register(KEY_CLONE_X_PLUS);
        event.register(KEY_CLONE_Y_MINUS);
        event.register(KEY_CLONE_Y_PLUS);
        event.register(KEY_CLONE_Z_MINUS);
        event.register(KEY_CLONE_Z_PLUS);
        event.register(KEY_CLONE_COUNT_MINUS);
        event.register(KEY_CLONE_COUNT_PLUS);
    }
}

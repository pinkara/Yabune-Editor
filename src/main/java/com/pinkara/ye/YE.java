package com.pinkara.ye;

import com.pinkara.ye.editor.EntityEditor;
import com.pinkara.ye.item.ItemEditor;
import com.pinkara.ye.network.YENetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(YE.MODID)
public class YE {
    public static final String MODID = "ye";
    public static final String MOD_NAME = "Yabune Editor";
    public static final String AUTHOR = "Pinkara";
    public static final String VERSION = "2.4.12-1.21.1-alpha13";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredItem<ItemEditor> EDITOR = ITEMS.register("editor", () -> new ItemEditor(new Item.Properties()));
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEditor>> ENTITY_EDITOR = ENTITY_TYPES.register("editor",
            () -> EntityType.Builder.<EntityEditor>of(EntityEditor::new, MobCategory.MISC)
                    .sized(0.0f, 0.0f)
                    .build("editor"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> YE_TAB = CREATIVE_MODE_TABS.register("ye_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ye"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> new ItemStack(EDITOR.get()))
                    .displayItems((params, output) -> output.accept(EDITOR.get()))
                    .build());

    public static boolean canUseEditor(net.minecraft.server.level.ServerPlayer player) {
        return player != null && player.isCreative();
    }

    public YE(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        YENetwork.register(modEventBus);
    }
}

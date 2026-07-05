package com.pinkara.ye.editor;

import com.pinkara.ye.YE;
import com.pinkara.ye.network.PacketRenderBlocks;
import com.pinkara.youma.block.BlockUtil;
import com.pinkara.youma.block.NGTObject;
import com.pinkara.youma.item.ItemUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public class EntityEditor extends Entity {
    private static final byte XZ_MASK_BIT = 9;
    private static final int XZ_OFFSET = 256;

    public static final EntityDataAccessor<String> PLAMCTER = SynchedEntityData.defineId(EntityEditor.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<BlockPos> START_POS = SynchedEntityData.defineId(EntityEditor.class, EntityDataSerializers.BLOCK_POS);
    public static final EntityDataAccessor<BlockPos> END_POS = SynchedEntityData.defineId(EntityEditor.class, EntityDataSerializers.BLOCK_POS);
    public static final EntityDataAccessor<BlockPos> PASTE_BOX = SynchedEntityData.defineId(EntityEditor.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> CLONE_BOX = SynchedEntityData.defineId(EntityEditor.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(EntityEditor.class, EntityDataSerializers.BYTE);

    private Player player;
    private final ItemStack[] slots = ItemUtil.getEmptyArray(2);
    public int fillMode = 0;

    public NGTObject blocksForRenderer;
    private boolean shouldUpdate = true;

    public EntityEditor(EntityType<?> type, Level world) {
        super(type, world);
        this.noPhysics = true;
    }

    protected EntityEditor(Level world, Player player, int x, int y, int z) {
        this(YE.ENTITY_EDITOR.get(), world);
        this.setPlayer(player);
        this.setPos(START_POS, x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PLAMCTER, "");
        builder.define(START_POS, BlockPos.ZERO);
        builder.define(END_POS, BlockPos.ZERO);
        builder.define(PASTE_BOX, BlockPos.ZERO);
        // Default clone box: offset X=5, Y=0, Z=0, count=1
        // Encoded as setCloneBox(5, 0, 0, 1)
        builder.define(CLONE_BOX, new BlockPos(773, 0, 256));
        builder.define(MODE, (byte) 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == START_POS && this.level().isClientSide) {
            // A new starting point invalidates the client-side copied preview.
            this.blocksForRenderer = null;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getPlayer() != null) {
            this.setPos(this.getPlayer().getX(), this.getPlayer().getY(), this.getPlayer().getZ());
        }
        if (!this.level().isClientSide) {
            if (this.getPlayer() == null || !this.getPlayer().isAlive()) {
                this.discard();
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            EditorManager.INSTANCE.remove(this);
        }
        super.remove(reason);
    }

    @Override
    public void lerpTo(double par1, double par3, double par5, float par7, float par8, int par9) {
    }

    public HitResult getTarget(boolean selectSide) {
        Player player = this.getPlayer();
        if (player != null) {
            byte mode = this.getEditMode();
            boolean flag = mode == 0 || mode == 2;
            return com.pinkara.ye.item.ItemEditor.getTarget(player, flag, selectSide);
        }
        return null;
    }

    public Player getPlayer() {
        if (this.player == null) {
            String name = this.entityData.get(PLAMCTER);
            if (!name.isEmpty()) {
                if (this.level() instanceof ServerLevel server) {
                    this.player = server.getServer().getPlayerList().getPlayerByName(name);
                } else if (this.level().isClientSide) {
                    this.player = net.minecraft.client.Minecraft.getInstance().player;
                }
            }
        }
        return this.player;
    }

    public void setPlayer(Player par1) {
        this.player = par1;
        this.entityData.set(PLAMCTER, par1.getGameProfile().getName());
    }

    public int[] getPos(EntityDataAccessor<BlockPos> type) {
        return BlockUtil.toArray(this.entityData.get(type));
    }

    public void setPos(EntityDataAccessor<BlockPos> type, int x, int y, int z) {
        if (type == START_POS) {
            this.entityData.set(END_POS, BlockPos.ZERO);
            this.blocksForRenderer = null;
        }
        this.entityData.set(type, new BlockPos(x, y, z));
    }

    public boolean isSelectEnd() {
        int[] start = this.getPos(START_POS);
        int[] end = this.getPos(END_POS);
        return start[1] > 0 && end[1] > 0;
    }

    public int[] getCloneBox() {
        int[] ia = this.getPos(CLONE_BOX);
        int mask = 511;
        int x = (ia[0] & mask) - 256;
        int z = (ia[2] & mask) - 256;
        int r = (ia[0] >> 9) + (ia[2] >> 9 << 4);
        return new int[]{x, ia[1], z, r};
    }

    public void setCloneBox(int x, int y, int z, int r) {
        r = Mth.clamp(r, 0, 255);
        x = Mth.clamp(x, -256, 255) + 256 + ((r & 0xF) << 9);
        z = Mth.clamp(z, -256, 255) + 256 + (r >> 4 << 9);
        this.setPos(CLONE_BOX, x, y, z);
    }

    public boolean hasCloneBox() {
        int[] ia = this.getCloneBox();
        return ia[3] > 0;
    }

    public byte getEditMode() {
        return this.entityData.get(MODE);
    }

    public void setEditMode(byte par1) {
        this.entityData.set(MODE, par1);
    }

    public net.minecraft.world.level.block.state.BlockState getSlotBlockState(int index) {
        ItemStack stack = this.slots[index];
        if (stack.isEmpty()) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return net.minecraft.world.level.block.Block.byItem(stack.getItem()).defaultBlockState();
    }

    public void setSlot(int index, ItemStack stack) {
        this.slots[index] = stack;
    }

    public ItemStack getEditorSlot(int index) {
        return this.slots[index];
    }

    public void updateBlockList(NGTObject ngto) {
        if (!this.level().isClientSide && this.getPlayer() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new PacketRenderBlocks(ngto));
        }
    }

    public void setUpdate(boolean par1) {
        this.shouldUpdate = par1;
    }

    public boolean shouldUpdate() {
        return this.shouldUpdate;
    }

    @Override
    protected void checkInsideBlocks() {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
}

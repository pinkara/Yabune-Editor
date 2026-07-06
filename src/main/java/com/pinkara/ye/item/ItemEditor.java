package com.pinkara.ye.item;

import com.pinkara.ye.editor.Editor;
import com.pinkara.ye.editor.EditorManager;
import com.pinkara.ye.editor.EntityEditor;
import com.pinkara.youma.block.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.pinkara.ye.editor.EntityEditor;

public class ItemEditor extends Item {
    public ItemEditor(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player == null || world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (EditorManager.INSTANCE.canPlayerUseEditor(player)) {
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor != null) {
                EntityEditor entity = editor.getEntity();
                if (entity == null || !entity.isAlive() || entity.level() != world) {
                    EditorManager.INSTANCE.remove(player);
                    editor = null;
                }
            }
            if (editor != null) {
                EntityDataAccessor<BlockPos> dp = editor.getEntity().isSelectEnd() ? EntityEditor.START_POS : EntityEditor.END_POS;
                editor.getEntity().setPos(dp, pos.getX(), pos.getY(), pos.getZ());
            } else {
                EntityEditor entityEditor = Editor.getNewEditor(world, player, pos.getX(), pos.getY(), pos.getZ());
                if (entityEditor != null) {
                            entityEditor.setPos(player.getX(), player.getY(), player.getZ());
                    entityEditor.setYRot(0.0f);
                    entityEditor.setXRot(0.0f);
                    world.addFreshEntity(entityEditor);
                }
            }
        } else {
            ((ServerPlayer) player).sendSystemMessage(Component.translatable("You don't have permission to use Editor."));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Editor editor = EditorManager.INSTANCE.getEditor(player);
        if (editor != null) {
            HitResult target = editor.getEntity().getTarget(false);
            if (target != null && target.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = ((BlockHitResult) target).getBlockPos();
                EntityDataAccessor<BlockPos> dp = editor.getEntity().isSelectEnd() ? EntityEditor.START_POS : EntityEditor.END_POS;
                editor.getEntity().setPos(dp, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static HitResult getTarget(Player player, boolean par2, boolean selectSide) {
        if (par2) {
            HitResult target = BlockUtil.getMOPFromPlayer(player, 128.0, true);
            if (target != null && target.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) target;
                BlockPos pos = blockHit.getBlockPos();
                if (selectSide) {
                    pos = pos.relative(blockHit.getDirection());
                }
                return new BlockHitResult(blockHit.getLocation(), blockHit.getDirection(), pos, blockHit.isInside());
            }
        } else {
            float f = 1.0f;
            float pitch = Mth.lerp(f, player.xRotO, player.getXRot());
            float yaw = Mth.lerp(f, player.yRotO, player.getYRot());
            double dx = Mth.lerp(f, player.xo, player.getX());
            double dy = Mth.lerp(f, player.yo, player.getY()) + player.getEyeHeight();
            double dz = Mth.lerp(f, player.zo, player.getZ());
            Vec3 vec3 = new Vec3(dx, dy, dz);
            float f3 = com.pinkara.youma.math.NGTMath.cos((float) (-yaw - 180.0f));
            float f4 = com.pinkara.youma.math.NGTMath.sin((float) (-yaw - 180.0f));
            float f5 = -com.pinkara.youma.math.NGTMath.cos((float) (-pitch));
            float f6 = com.pinkara.youma.math.NGTMath.sin((float) (-pitch));
            float x2 = f4 * f5;
            float z2 = f3 * f5;
            double distance = 8.0;
            Vec3 vec31 = vec3.add((double) x2 * distance, (double) f6 * distance, (double) z2 * distance);
            int x = com.pinkara.youma.math.NGTMath.floor(vec31.x);
            int y = com.pinkara.youma.math.NGTMath.floor(vec31.y);
            int z = com.pinkara.youma.math.NGTMath.floor(vec31.z);
            if (y >= 0) {
                if (y > 319) {
                    y = 319;
                }
                return new BlockHitResult(vec31, Direction.UP, new BlockPos(x, y, z), false);
            }
        }
        return null;
    }
}

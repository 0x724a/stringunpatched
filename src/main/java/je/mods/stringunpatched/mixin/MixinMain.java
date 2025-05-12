package je.mods.stringunpatched.mixin;

import com.google.common.base.MoreObjects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Optional;

import static net.minecraft.world.level.block.TripWireHookBlock.*;

@Mixin(TripWireHookBlock.class)
public class MixinMain {
    @Inject(method = "calculateState", at = @At("HEAD"), cancellable = true)
    private static void calculateState(Level level, BlockPos pos, BlockState hookState, boolean attaching, boolean shouldNotifyNeighbours, int searchRange, @Nullable BlockState state, CallbackInfo ci) {
        Optional<Direction> optional = hookState.getOptionalValue(FACING);
        if (optional.isPresent()) {
            Direction direction = (Direction)optional.get();
            boolean flag = (Boolean)hookState.getOptionalValue(ATTACHED).orElse(false);
            boolean flag1 = (Boolean)hookState.getOptionalValue(POWERED).orElse(false);
            Block block = hookState.getBlock();
            boolean flag2 = !attaching;
            boolean flag3 = false;
            int i = 0;
            BlockState[] ablockstate = new BlockState[42];

            BlockPos blockpos1;
            for(int j = 1; j < 42; ++j) {
                blockpos1 = pos.relative(direction, j);
                BlockState blockstate = level.getBlockState(blockpos1);
                if (blockstate.is(Blocks.TRIPWIRE_HOOK)) {
                    if (blockstate.getValue(FACING) == direction.getOpposite()) {
                        i = j;
                    }
                    break;
                }

                if (!blockstate.is(Blocks.TRIPWIRE) && j != searchRange) {
                    ablockstate[j] = null;
                    flag2 = false;
                } else {
                    if (j == searchRange) {
                        blockstate = (BlockState) MoreObjects.firstNonNull(state, blockstate);
                    }

                    boolean flag4 = !(Boolean)blockstate.getValue(TripWireBlock.DISARMED);
                    boolean flag5 = (Boolean)blockstate.getValue(TripWireBlock.POWERED);
                    flag3 |= flag4 && flag5;
                    ablockstate[j] = blockstate;
                    if (j == searchRange) {
                        level.scheduleTick(pos, block, 10);
                        flag2 &= flag4;
                    }
                }
            }

            flag2 &= i > 1;
            flag3 &= flag2;
            BlockState blockstate1 = (BlockState)((BlockState)block.defaultBlockState().trySetValue(ATTACHED, flag2)).trySetValue(POWERED, flag3);
            if (i > 0) {
                blockpos1 = pos.relative(direction, i);
                Direction direction1 = direction.getOpposite();
                level.setBlock(blockpos1, (BlockState)blockstate1.setValue(FACING, direction1), 3);
                notifyNeighbors(block, level, blockpos1, direction1);
                emitState(level, blockpos1, flag2, flag3, flag, flag1);
            }

            emitState(level, pos, flag2, flag3, flag, flag1);
            if (!attaching) {
                level.setBlock(pos, (BlockState)blockstate1.setValue(FACING, direction), 3);
                if (shouldNotifyNeighbours) {
                    notifyNeighbors(block, level, pos, direction);
                }
            }

            if (flag != flag2) {
                for(int k = 1; k < i; ++k) {
                    BlockPos blockpos2 = pos.relative(direction, k);
                    BlockState blockstate2 = ablockstate[k];
                    if (blockstate2 != null) {
                        level.setBlock(blockpos2, (BlockState)blockstate2.trySetValue(ATTACHED, flag2), 3);
                    }
                }
            }
            ci.cancel();
        }
    }
    @Shadow
    private static void emitState(Level level, BlockPos pos, boolean attached, boolean powered, boolean wasAttached, boolean wasPowered) {
        if (powered && !wasPowered) {
            level.playSound((Player)null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.4F, 0.6F);
            level.gameEvent((Entity)null, GameEvent.BLOCK_ACTIVATE, pos);
        } else if (!powered && wasPowered) {
            level.playSound((Player)null, pos, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, 0.4F, 0.5F);
            level.gameEvent((Entity)null, GameEvent.BLOCK_DEACTIVATE, pos);
        } else if (attached && !wasAttached) {
            level.playSound((Player)null, pos, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.4F, 0.7F);
            level.gameEvent((Entity)null, GameEvent.BLOCK_ATTACH, pos);
        } else if (!attached && wasAttached) {
            level.playSound((Player)null, pos, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 0.4F, 1.2F / (level.random.nextFloat() * 0.2F + 0.9F));
            level.gameEvent((Entity)null, GameEvent.BLOCK_DETACH, pos);
        }

    }
    @Shadow
    private static void notifyNeighbors(Block block, Level level, BlockPos pos, Direction direction) {
        level.updateNeighborsAt(pos, block);
        level.updateNeighborsAt(pos.relative(direction.getOpposite()), block);
    }
}

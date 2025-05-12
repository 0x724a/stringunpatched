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

import javax.annotation.Nullable;

import static net.minecraft.world.level.block.TripWireHookBlock.*;


@Mixin(TripWireHookBlock.class)
public class MixinMain extends Block {

    public MixinMain(Properties arg) {
        super(arg);
    }

    @Inject(method = "calculateState", at = @At("HEAD"), cancellable = true)
    private void calculateState(Level arg, BlockPos arg2, BlockState arg3, boolean bl, boolean bl2, int l, @Nullable BlockState arg4, CallbackInfo ci) {
        Direction direction = (Direction)arg3.getValue(FACING);
        boolean flag = (Boolean)arg3.getValue(ATTACHED);
        boolean flag1 = (Boolean)arg3.getValue(POWERED);
        boolean flag2 = !bl;
        boolean flag3 = false;
        int i = 0;
        BlockState[] ablockstate = new BlockState[42];

        BlockPos blockpos1;
        for(int j = 1; j < 42; ++j) {
            blockpos1 = arg2.relative(direction, j);
            BlockState blockstate = arg.getBlockState(blockpos1);
            if (blockstate.is(Blocks.TRIPWIRE_HOOK)) {
                if (blockstate.getValue(FACING) == direction.getOpposite()) {
                    i = j;
                }
                break;
            }

            if (!blockstate.is(Blocks.TRIPWIRE) && j != l) {
                ablockstate[j] = null;
                flag2 = false;
            } else {
                if (j == l) {
                    blockstate = (BlockState)MoreObjects.firstNonNull(arg4, blockstate);
                }

                boolean flag4 = !(Boolean)blockstate.getValue(TripWireBlock.DISARMED);
                boolean flag5 = (Boolean)blockstate.getValue(TripWireBlock.POWERED);
                flag3 |= flag4 && flag5;
                ablockstate[j] = blockstate;
                if (j == l) {
                    arg.scheduleTick(arg2, this, 10);
                    flag2 &= flag4;
                }
            }
        }

        flag2 &= i > 1;
        flag3 &= flag2;
        BlockState blockstate1 = (BlockState)((BlockState)this.defaultBlockState().setValue(ATTACHED, flag2)).setValue(POWERED, flag3);
        if (i > 0) {
            blockpos1 = arg2.relative(direction, i);
            Direction direction1 = direction.getOpposite();
            arg.setBlock(blockpos1, (BlockState)blockstate1.setValue(FACING, direction1), 3);
            this.notifyNeighbors(arg, blockpos1, direction1);
            this.playSound(arg, blockpos1, flag2, flag3, flag, flag1);
        }

        this.playSound(arg, arg2, flag2, flag3, flag, flag1);
        if (!bl) {
            arg.setBlock(arg2, (BlockState)blockstate1.setValue(FACING, direction), 3);
            if (bl2) {
                this.notifyNeighbors(arg, arg2, direction);
            }
        }

        if (flag != flag2) {
            for(int k = 1; k < i; ++k) {
                BlockPos blockpos2 = arg2.relative(direction, k);
                BlockState blockstate2 = ablockstate[k];
                arg.setBlock(blockpos2, (BlockState)blockstate2.setValue(ATTACHED, flag2), 3);
            }
        }

        ci.cancel();
    }
    @Shadow
    private void playSound(Level arg, BlockPos arg2, boolean bl, boolean bl2, boolean bl3, boolean bl4) {
        if (bl2 && !bl4) {
            arg.playSound((Player)null, arg2, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.4F, 0.6F);
            arg.gameEvent(GameEvent.BLOCK_PRESS, arg2);
        } else if (!bl2 && bl4) {
            arg.playSound((Player)null, arg2, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, 0.4F, 0.5F);
            arg.gameEvent(GameEvent.BLOCK_UNPRESS, arg2);
        } else if (bl && !bl3) {
            arg.playSound((Player)null, arg2, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.4F, 0.7F);
            arg.gameEvent(GameEvent.BLOCK_ATTACH, arg2);
        } else if (!bl && bl3) {
            arg.playSound((Player)null, arg2, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 0.4F, 1.2F / (arg.random.nextFloat() * 0.2F + 0.9F));
            arg.gameEvent(GameEvent.BLOCK_DETACH, arg2);
        }

    }
    @Shadow
    private void notifyNeighbors(Level arg, BlockPos arg2, Direction arg3) {
        arg.updateNeighborsAt(arg2, this);
        arg.updateNeighborsAt(arg2.relative(arg3.getOpposite()), this);
    }

}
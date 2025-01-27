package kogasastudio.ashihara.block;

import kogasastudio.ashihara.client.particles.GenericParticleData;
import kogasastudio.ashihara.client.particles.ParticleRegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS;

public class BlockTatami extends Block {
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty XCUT = BooleanProperty.create("xcut");
    public static final BooleanProperty ZCUT = BooleanProperty.create("zcut");
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");
    public static final EnumProperty<Direction.Axis> AXIS = HORIZONTAL_AXIS;
    public BlockTatami() {
        super
                (
                        Properties.of(Material.BAMBOO_SAPLING)
                                .strength(0.3F)
                                .sound(SoundType.BAMBOO_SAPLING)
                );
        this.registerDefaultState
                (
                        getStateDefinition().any()
                                .setValue(LEFT, false)
                                .setValue(RIGHT, false)
                                .setValue(XCUT, false)
                                .setValue(ZCUT, false)
                                .setValue(LOCKED, false)
                                .setValue(AXIS, Direction.Axis.X)
                );
    }

    //获取和更新bs，输入操作前的bs，返回操作后的bs
    //获取8个方向相邻的方块对其判定，若判定结果为true则取消这一点上的边缘权重
    //随后由multipart模型对其进行模型更新
    private BlockState updateState(BlockState state, Level worldIn, BlockPos pos) {
        if (state.is(BlockRegistryHandler.TATAMI.get()) && !state.getValue(LOCKED)) {
            BlockState n = worldIn.getBlockState(pos.north());
            BlockState s = worldIn.getBlockState(pos.south());
            BlockState e = worldIn.getBlockState(pos.east());
            BlockState w = worldIn.getBlockState(pos.west());

            boolean isX = state.getValue(AXIS).equals(Direction.Axis.X);

            state = state.setValue(LEFT, check(isX ? n : e, state))
                    .setValue(RIGHT, check(isX ? s : w, state));
        }
        return state;
    }

    //检查传入的方块是否是榻榻米且轴与判断源榻榻米方块相同
    private boolean check(BlockState state, BlockState newState) {
        return state.is(BlockRegistryHandler.TATAMI.get()) && state.getValue(AXIS) == newState.getValue(AXIS);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEFT, RIGHT, XCUT, ZCUT, LOCKED, AXIS);
    }

    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        BlockState fromState = worldIn.getBlockState(fromPos);
        if (fromState.is(BlockRegistryHandler.TATAMI.get()) || fromState.is(Blocks.AIR)) {
            worldIn.setBlockAndUpdate(pos, updateState(state, worldIn, pos));
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level worldIn = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return updateState(this.defaultBlockState(), worldIn, pos).setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    //空手shift右键锁定，剪刀右键加中央边缘权重（剪开或合上）
    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (state.is(BlockRegistryHandler.TATAMI.get())) {
            if (player.getItemInHand(handIn).isEmpty() && player.isShiftKeyDown()) {
                worldIn.setBlockAndUpdate(pos, state.setValue(LOCKED, !state.getValue(LOCKED)));
                worldIn.playSound(player, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                for (byte b = 0; b < 12; b += 1) {
                    worldIn.addParticle(new GenericParticleData(new Vec3(0, 0, 0), 0, ParticleRegistryHandler.RICE.get()), (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, RANDOM.nextFloat() / 2.0F, 5.0E-5D, RANDOM.nextFloat() / 2.0F);
                }
                return InteractionResult.SUCCESS;
            } else if (player.getItemInHand(handIn).getItem() instanceof ShearsItem && !player.isShiftKeyDown()) {
                Direction direction = player.getDirection();
                if (direction.getAxis().equals(Direction.Axis.X)) {
                    worldIn.setBlockAndUpdate(pos, state.setValue(XCUT, !state.getValue(XCUT)));
                    worldIn.playSound(player, pos, SoundEvents.BAMBOO_SAPLING_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else if (direction.getAxis().equals(Direction.Axis.Z)) {
                    worldIn.setBlockAndUpdate(pos, state.setValue(ZCUT, !state.getValue(ZCUT)));
                    worldIn.playSound(player, pos, SoundEvents.BAMBOO_SAPLING_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            } else return InteractionResult.PASS;
        } else return InteractionResult.PASS;
    }
}

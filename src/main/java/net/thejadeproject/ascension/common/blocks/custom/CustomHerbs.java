package net.thejadeproject.ascension.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.Set;
import java.util.function.Supplier;

public class CustomHerbs extends FlowerBlock {
    private final Set<Block> blocksToSurviveOn;

    public CustomHerbs(Supplier<Set<Block>> blocksToSurviveOn) {
        super(MobEffects.MOVEMENT_SPEED, 100, Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).instabreak().noCollission().offsetType(OffsetType.XZ).pushReaction(PushReaction.DESTROY));
        this.blocksToSurviveOn = blocksToSurviveOn.get();
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return blocksToSurviveOn.contains(state.getBlock());
    }

    // Add this getter method to access the set of blocks
    public Set<Block> getBlocksToSurviveOn() {
        return blocksToSurviveOn;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 60;
    }
}
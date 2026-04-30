package net.thejadeproject.ascension.common.blocks.custom.crops;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.thejadeproject.ascension.common.items.data_components.ModDataComponents;
import net.thejadeproject.ascension.common.items.ModItems;
import net.thejadeproject.ascension.common.items.herbs.HerbQuality;

import java.util.ArrayList;
import java.util.List;

/**
 * Stem-shaped slow-growing crop for Ascension herbs.
 * Same Age & Quality logic as GenericSlowCropBlock — see that class for details.
 */
public class StemSlowCropBlock extends CropBlock {

    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    private static final VoxelShape[] SHAPE_BY_AGE = {
            Block.box(7, 0, 7, 9, 2, 9),
            Block.box(7, 0, 7, 9, 4, 9),
            Block.box(7, 0, 7, 9, 6, 9),
            Block.box(7, 0, 7, 9, 8, 9)
    };

    private final ItemLike seedItem;
    private final float    growthChance;

    public static StemSlowCropBlock createStemCrop(Properties properties) {
        return new StemSlowCropBlock(properties, ModItems.HUNDRED_YEAR_GINSENG, 0.001f);
    }

    public StemSlowCropBlock(Properties properties, ItemLike seedItem, float growthChance) {
        super(properties);
        this.seedItem     = seedItem;
        this.growthChance = growthChance;
    }

    public StemSlowCropBlock(Properties properties, ItemLike seedItem) {
        this(properties, seedItem, 0.001f);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE_BY_AGE[state.getValue(AGE)];
    }

    @Override protected ItemLike getBaseSeedId()       { return seedItem; }
    @Override public  IntegerProperty getAgeProperty() { return AGE; }
    @Override public  int getMaxAge()                  { return MAX_AGE; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) return;
        if (level.getRawBrightness(pos, 0) < 9) return;

        int currentAge = getAge(state);
        if (currentAge >= getMaxAge()) return;

        if (random.nextFloat() < growthChance) {
            int nextAge = currentAge + 1;
            level.setBlock(pos, getStateForAge(nextAge), 2);
            if (nextAge == getMaxAge()) {
                CropAgeCache.store(level, pos, level.getGameTime(), HerbQuality.rollQuality());
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        if (getAge(state) < getMaxAge()) return drops;

        ServerLevel level = null;
        BlockPos pos = null;
        try {
            if (builder.getLevel() instanceof ServerLevel sl) level = sl;
            var origin = builder.getOptionalParameter(
                    net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
            if (origin != null) pos = BlockPos.containing(origin);
        } catch (Exception ignored) {}

        long ageTicks = 0L;
        int  quality  = HerbQuality.BASIC;

        if (level != null && pos != null) {
            CropAgeCache.CropData data = CropAgeCache.retrieve(level, pos);
            if (data != null) {
                ageTicks = Math.max(0, level.getGameTime() - data.grownSince());
                quality  = data.quality();
                CropAgeCache.remove(level, pos);
            }
        }

        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                drop.set(ModDataComponents.HERB_AGE_TICKS.get(), ageTicks);
                drop.set(ModDataComponents.HERB_QUALITY.get(), quality);
            }
        }

        return drops;
    }

    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) { return false; }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return false; }
    @Override protected int getBonemealAgeIncrease(Level level) { return 0; }
}
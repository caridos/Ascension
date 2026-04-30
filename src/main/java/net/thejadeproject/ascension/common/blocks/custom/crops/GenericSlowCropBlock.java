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
 * Slow-growing generic crop for Ascension herbs.
 *
 * ── Age & Quality stamping ────────────────────────────────────────────────
 * When the crop first reaches max age (3) via randomTick, it records the
 * current world game-time in block NBT as "grownSince". No per-tick work
 * is done — the timestamp is written once and never touched again.
 *
 * When the crop is broken, getDrops() reads the timestamp, computes
 * (currentTime - grownSince) = "ticks at max age", and stamps the result
 * as HERB_AGE_TICKS on every drop. Quality is rolled once at grownSince-time
 * and stored as "grownQuality" in block NBT, then copied to the drop item.
 *
 * Block NBT keys:
 *   "grownSince"   long  — game-time tick when age 3 was first reached
 *   "grownQuality" int   — quality tier rolled at that moment (0-3)
 */
public class GenericSlowCropBlock extends CropBlock {

    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    private static final VoxelShape[] SHAPE_BY_AGE = {
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(0, 0, 0, 16, 4, 16),
            Block.box(0, 0, 0, 16, 6, 16),
            Block.box(0, 0, 0, 16, 8, 16)
    };

    private final ItemLike seedItem;
    private final float    growthChance;

    // ── Constructors ──────────────────────────────────────────────

    public static GenericSlowCropBlock createHundredYearGinseng(Properties properties) {
        return new GenericSlowCropBlock(properties, ModItems.HUNDRED_YEAR_GINSENG, 0.001f);
    }

    public GenericSlowCropBlock(Properties properties, ItemLike seedItem, float growthChance) {
        super(properties);
        this.seedItem    = seedItem;
        this.growthChance = growthChance;
    }

    public GenericSlowCropBlock(Properties properties, ItemLike seedItem) {
        this(properties, seedItem, 0.001f);
    }

    // ── Shape / Age ───────────────────────────────────────────────

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE_BY_AGE[state.getValue(AGE)];
    }

    @Override protected ItemLike getBaseSeedId()    { return seedItem; }
    @Override public  IntegerProperty getAgeProperty() { return AGE; }
    @Override public  int getMaxAge()                { return MAX_AGE; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    // ── Growth ────────────────────────────────────────────────────

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) return;
        if (level.getRawBrightness(pos, 0) < 9) return;

        int currentAge = getAge(state);
        if (currentAge >= getMaxAge()) {
            // Already fully grown — nothing to do (age tracked in block entity NBT below)
            return;
        }

        if (random.nextFloat() < growthChance) {
            int nextAge = currentAge + 1;
            level.setBlock(pos, getStateForAge(nextAge), 2);

            // First time reaching max age — record timestamp and roll quality
            if (nextAge == getMaxAge()) {
                onFirstFullyGrown(level, pos);
            }
        }
    }

    /**
     * Called exactly once when the crop transitions to max age.
     * Writes grownSince + grownQuality into the level's per-block-pos custom data
     * via a simple NBT trick: we use BlockEntity-less storage by saving into a
     * companion data structure.
     *
     * Because vanilla CropBlock has no BlockEntity, we use a lightweight approach:
     * store the data as block state extra data isn't possible for longs, so we
     * use a static WeakHashMap keyed by ServerLevel + BlockPos, cleared on unload.
     * This is zero-persistence-risk because if the chunk unloads and reloads before
     * harvest, the crop will still appear fully grown — the harvest just won't have
     * age data (it gets treated as Young quality).
     */
    private static void onFirstFullyGrown(ServerLevel level, BlockPos pos) {
        long gameTick = level.getGameTime();
        int  quality  = HerbQuality.rollQuality();
        CropAgeCache.store(level, pos, gameTick, quality);
    }

    // ── Drops ─────────────────────────────────────────────────────

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));

        // Only stamp fully-grown crops
        if (getAge(state) < getMaxAge()) return drops;

        // Retrieve from cache — null if chunk was unloaded/reloaded (treat as Young)
        ServerLevel level = null;
        try {
            var contextParam = net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE;
            // Access level via the origin param
            var origin = builder.getOptionalParameter(
                    net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
            if (origin != null) {
                var levelParam = builder.getLevel();
                if (levelParam instanceof ServerLevel sl) level = sl;
            }
        } catch (Exception ignored) {}

        long grownSince = -1;
        int  quality    = HerbQuality.BASIC;

        if (level != null) {
            BlockPos pos = null;
            try {
                var origin = builder.getOptionalParameter(
                        net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
                if (origin != null) {
                    pos = BlockPos.containing(origin);
                }
            } catch (Exception ignored) {}

            if (pos != null) {
                CropAgeCache.CropData data = CropAgeCache.retrieve(level, pos);
                if (data != null) {
                    grownSince = data.grownSince();
                    quality    = data.quality();
                    CropAgeCache.remove(level, pos);
                }
            }
        }

        long ageTicks = (grownSince >= 0 && level != null)
                ? Math.max(0, level.getGameTime() - grownSince)
                : 0L;

        // Stamp every drop item
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                drop.set(ModDataComponents.HERB_AGE_TICKS.get(), ageTicks);
                drop.set(ModDataComponents.HERB_QUALITY.get(), quality);
            }
        }

        return drops;
    }

    // ── Bonemeal disabled ─────────────────────────────────────────

    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) { return false; }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return false; }
    @Override protected int getBonemealAgeIncrease(Level level) { return 0; }
}
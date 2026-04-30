package net.thejadeproject.ascension.refactor_packages.events.physiques;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.thejadeproject.ascension.AscensionCraft;
import net.thejadeproject.ascension.data_attachments.ModAttachments;
import net.thejadeproject.ascension.refactor_packages.entity_data.IEntityData;
import net.thejadeproject.ascension.refactor_packages.paths.ModPaths;
import net.thejadeproject.ascension.refactor_packages.paths.PathData;
import net.thejadeproject.ascension.refactor_packages.physiques.ModPhysiques;
import net.thejadeproject.ascension.refactor_packages.physiques.custom.helpers.PhysiqueEvolutionHelper;
import net.thejadeproject.ascension.refactor_packages.util.PhysiqueEvolutionEventUtil;

@EventBusSubscriber(modid = AscensionCraft.MOD_ID)
public final class PacifistPhysiqueEvolutionEvents {

    private PacifistPhysiqueEvolutionEvents() {}

    // Blessed and Virtuoso Buddha Physique Evolution Methods
    private static final String TAG_ROOT = "ascension_pacifist_physique";
    private static final String TAG_PEACE_START_TICK = "peace_start_tick";
    private static final String TAG_BLESSED_PACIFIST_BROKEN = "blessed_pacifist_broken";

    private static final long TICKS_PER_SECOND = 20L;
    private static final long TICKS_PER_MINECRAFT_DAY = 24000L;

    private static final long MORTAL_PEACE_TICKS_REQUIRED = 14L * TICKS_PER_MINECRAFT_DAY;

    private static final int VIRTUOSO_REQUIRED_BODY_MAJOR_REALM = 0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (player.tickCount % TICKS_PER_SECOND != 0) return;
        if (!player.hasData(ModAttachments.ENTITY_DATA)) return;

        IEntityData entityData = player.getData(ModAttachments.ENTITY_DATA);

        if (PhysiqueEvolutionEventUtil.hasPhysique(entityData, ModPhysiques.MORTAL.getId())) {
            tickMortalPacifistProgress(player, entityData);
            return;
        }

        if (PhysiqueEvolutionEventUtil.hasPhysique(entityData, ModPhysiques.BLESSED.getId())) {
            tickBlessedPacifistProgress(player, entityData);
            return;
        }

        clearPacifistTracking(player);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();

        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        if (victim == killer) return;
        if (!killer.hasData(ModAttachments.ENTITY_DATA)) return;

        IEntityData entityData = killer.getData(ModAttachments.ENTITY_DATA);
        CompoundTag tag = getPacifistTag(killer);

        if (PhysiqueEvolutionEventUtil.hasPhysique(entityData, ModPhysiques.MORTAL.getId())) {
            tag.putLong(TAG_PEACE_START_TICK, killer.level().getGameTime());

            killer.sendSystemMessage(
                    Component.translatable("ascension.message.physique_evolution.pacifist_mortal_reset")
            );
            return;
        }

        if (PhysiqueEvolutionEventUtil.hasPhysique(entityData, ModPhysiques.BLESSED.getId())) {
            tag.putBoolean(TAG_BLESSED_PACIFIST_BROKEN, true);

            killer.sendSystemMessage(
                    Component.translatable("ascension.message.physique_evolution.pacifist_blessed_broken")
            );
        }
    }

    private static void tickMortalPacifistProgress(ServerPlayer player, IEntityData entityData) {
        CompoundTag tag = getPacifistTag(player);

        long now = player.level().getGameTime();

        if (!tag.contains(TAG_PEACE_START_TICK, Tag.TAG_LONG)) {
            tag.putLong(TAG_PEACE_START_TICK, now);
            tag.putBoolean(TAG_BLESSED_PACIFIST_BROKEN, false);
            return;
        }

        long peaceStartTick = tag.getLong(TAG_PEACE_START_TICK);
        long elapsedTicks = now - peaceStartTick;

        if (elapsedTicks < MORTAL_PEACE_TICKS_REQUIRED) return;

        boolean evolved = PhysiqueEvolutionHelper.tryEvolveInto(
                player,
                entityData,
                ModPhysiques.BLESSED.getId()
        );

        if (evolved) {
            tag.putLong(TAG_PEACE_START_TICK, now);
            tag.putBoolean(TAG_BLESSED_PACIFIST_BROKEN, false);
        }
    }

    private static void tickBlessedPacifistProgress(ServerPlayer player, IEntityData entityData) {
        CompoundTag tag = getPacifistTag(player);

        if (tag.getBoolean(TAG_BLESSED_PACIFIST_BROKEN)) return;

        PathData bodyData = entityData.getPathData(ModPaths.BODY.getId());
        if (bodyData == null) return;

        if (bodyData.getMajorRealm() < VIRTUOSO_REQUIRED_BODY_MAJOR_REALM) return;

        boolean evolved = PhysiqueEvolutionHelper.tryEvolveInto(
                player,
                entityData,
                ModPhysiques.VIRTUOSO_BUDDHA.getId()
        );

        if (evolved) {
            clearPacifistTracking(player);
        }
    }

    private static CompoundTag getPacifistTag(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();

        if (!persistentData.contains(TAG_ROOT, Tag.TAG_COMPOUND)) {
            persistentData.put(TAG_ROOT, new CompoundTag());
        }

        return persistentData.getCompound(TAG_ROOT);
    }

    private static void clearPacifistTracking(ServerPlayer player) {
        CompoundTag tag = getPacifistTag(player);

        tag.remove(TAG_PEACE_START_TICK);
        tag.remove(TAG_BLESSED_PACIFIST_BROKEN);
    }


}
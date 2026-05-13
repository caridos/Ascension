package net.thejadeproject.ascension.mob_cultivation.util;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;
import net.thejadeproject.ascension.data_attachments.ModAttachments;
import net.thejadeproject.ascension.mob_cultivation.*;
import net.thejadeproject.ascension.refactor_packages.network.client_bound.mob_culti.SyncMobCultivation;

public final class MobCultivationCommandHelper {
    private MobCultivationCommandHelper() {}

    public static boolean applyCultivation(LivingEntity entity, String realmId, int stage) {
        if (!MobCultivationList.isValidRealm(realmId)) {
            return false;
        }

        MobCultivationData data = entity.getData(ModAttachments.MOB_RANK);
        if (data == null) return false;

        data.setRealmId(realmId);
        data.setStage(Math.max(1, Math.min(3, stage)));
        data.setInitialized(true);

        MobCultivationApplier.applyFromData(entity, data);
        sync(entity);
        return true;
    }

    public static Component getStatsMessage(LivingEntity entity) {
        MobCultivationData data = entity.getData(ModAttachments.MOB_RANK);

        if (data == null || !data.isInitialized()) {
            return Component.literal(entity.getName().getString() + " has no initialized mob cultivation.");
        }

        MobCultivationDefinition definition = MobCultivationResolver.resolveDefinition(data);
        MobCultivationStatProfile stats = MobCultivationResolver.resolveFinalStats(entity, definition);
        MobCultivationCategory category = MobCultivationResolver.resolveCategory(entity);

        return Component.literal(
                "\n=== Mob Cultivation Stats ==="
                        + "\nName: " + entity.getName().getString()
                        + "\nCategory: " + category
                        + "\nRealm: " + data.getRealmId()
                        + "\nStage: " + data.getStage()

                        + "\n\n--- Cultivation Stats ---"
                        + "\nVitality: " + fmt(stats.vitality())
                        + "\nStrength: " + fmt(stats.strength())
                        + "\nAgility: " + fmt(stats.agility())

                        + "\n\n--- Entity Stats ---"
                        + "\nCurrent Health: " + fmt(entity.getHealth()) + " / " + fmt(entity.getMaxHealth())
                        + "\nMax Health: " + attr(entity, Attributes.MAX_HEALTH)
                        + "\nAttack Damage: " + attr(entity, Attributes.ATTACK_DAMAGE)
                        + "\nMovement Speed: " + attr(entity, Attributes.MOVEMENT_SPEED)
                        + "\nArmor: " + attr(entity, Attributes.ARMOR)
                        + "\nArmor Toughness: " + attr(entity, Attributes.ARMOR_TOUGHNESS)
                        + "\nSafe Fall Distance: " + attr(entity, Attributes.SAFE_FALL_DISTANCE)
                        + "\nWater Movement Efficiency: " + attr(entity, Attributes.WATER_MOVEMENT_EFFICIENCY)
        );
    }

    private static void sync(LivingEntity entity) {
        if (entity.level().isClientSide()) return;

        MobCultivationData data = entity.getData(ModAttachments.MOB_RANK);
        if (data == null) return;

        PacketDistributor.sendToPlayersTrackingEntity(
                entity,
                new SyncMobCultivation(
                        entity.getId(),
                        data.getRealmId(),
                        data.getStage(),
                        data.isInitialized()
                )
        );
    }

    private static String attr(LivingEntity entity, Holder<Attribute> attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return "N/A";
        }

        return fmt(instance.getValue()) + " base: " + fmt(instance.getBaseValue());
    }

    private static String fmt(double value) {
        return String.format("%.2f", value);
    }
}
package net.thejadeproject.ascension.mob_cultivation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.neoforge.common.Tags;

public class MobCategoryResolver {

    private MobCategoryResolver() {}

    public static MobCultivationCategory resolveCategory(LivingEntity entity) {
        if (entity.getType().is(Tags.EntityTypes.BOSSES)) {
            return MobCultivationCategory.BOSS;
        }

        if (entity instanceof Enemy) {
            return MobCultivationCategory.HOSTILE;
        }

        return MobCultivationCategory.PASSIVE;
    }

}

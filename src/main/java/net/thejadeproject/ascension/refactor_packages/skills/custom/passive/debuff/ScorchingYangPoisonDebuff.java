package net.thejadeproject.ascension.refactor_packages.skills.custom.passive.debuff;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.thejadeproject.ascension.refactor_packages.entity_data.IEntityData;
import net.thejadeproject.ascension.refactor_packages.skills.ITickingSkill;
import net.thejadeproject.ascension.refactor_packages.skills.custom.passive.SimplePassiveSkill;

public class ScorchingYangPoisonDebuff extends SimplePassiveSkill implements ITickingSkill {

    private static final int TICK_INTERVAL = 20;
    private static final float DAMAGE_PER_TICK = 1.0F;

    private int tickCounter = 0;

    @Override
    public void onPlayerTick(ServerPlayer player, IEntityData entityData) {
        tickCounter++;

        if (tickCounter % TICK_INTERVAL != 0) {
            return;
        }

        player.invulnerableTime = 0;

        if (player.fireImmune()) {
            player.hurt(player.damageSources().magic(), DAMAGE_PER_TICK);
        } else {
            DamageSource internalFire = new DamageSource(
                    player.level().registryAccess()
                            .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(DamageTypes.ON_FIRE)
            );
            player.hurt(internalFire, DAMAGE_PER_TICK);
        }

        player.setRemainingFireTicks(20);
    }

    @Override
    protected String getTitleKey() {
        return "ascension.skill.scorching_yang_debuff";
    }

    @Override
    protected String getDescriptionKey() {
        return "ascension.skill.scorching_yang_debuff.description";
    }
}
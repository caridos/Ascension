package net.thejadeproject.ascension.refactor_packages.skills.custom.passive.debuff;

import net.minecraft.server.level.ServerPlayer;
import net.thejadeproject.ascension.refactor_packages.entity_data.IEntityData;
import net.thejadeproject.ascension.refactor_packages.qi.EntityQiContainer;
import net.thejadeproject.ascension.refactor_packages.skills.ITickingSkill;
import net.thejadeproject.ascension.refactor_packages.skills.custom.passive.SimplePassiveSkill;

public class QiDevouringPoisonDebuff extends SimplePassiveSkill implements ITickingSkill {

    private int tickCounter = 0;

    /**
     * Amplifier controls both drain severity and tick interval,
     * mirroring the old MobEffect behaviour:
     *   interval = max(10, 20 - amplifier * 3)
     *   drainAmount = (amplifier + 1) * 25
     */
    private int amplifier = 0;

    @Override
    public void onPlayerTick(ServerPlayer player, IEntityData entityData) {
        int interval = Math.max(10, 20 - (amplifier * 3));
        tickCounter++;

        if (tickCounter % interval != 0) {
            return;
        }

        int drainAmount = (amplifier + 1) * 25;

        EntityQiContainer qi = entityData.getQiContainer();
        double currentQi = qi.getCurrentQi();

        if (currentQi >= drainAmount) {
            qi.tryConsumeQi(drainAmount);
        } else {
            qi.tryConsumeQi(currentQi);
            float healthDamage = (float) (drainAmount - currentQi);
            player.hurt(player.damageSources().magic(), healthDamage);
        }
    }

    @Override
    protected String getTitleKey() {
        return "ascension.skill.qi_devouring_poison_debuff";
    }

    @Override
    protected String getDescriptionKey() {
        return "ascension.skill.qi_devouring_poison_debuff.description";
    }
}
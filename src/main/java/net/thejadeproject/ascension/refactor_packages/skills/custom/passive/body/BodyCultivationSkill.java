package net.thejadeproject.ascension.refactor_packages.skills.custom.passive.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.thejadeproject.ascension.data_attachments.ModAttachments;
import net.thejadeproject.ascension.refactor_packages.entity_data.IEntityData;
import net.thejadeproject.ascension.refactor_packages.paths.ModPaths;
import net.thejadeproject.ascension.refactor_packages.paths.PathData;
import net.thejadeproject.ascension.refactor_packages.skills.custom.passive.SimplePassiveSkill;

public class BodyCultivationSkill extends SimplePassiveSkill {

    public BodyCultivationSkill() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        float damage = event.getNewDamage();
        if (damage < 10.0f) {
            return;
        }

        IEntityData entityData = player.getData(ModAttachments.ENTITY_DATA);
        PathData bodyPath = entityData.getPathData(ModPaths.BODY.getId());
        if (bodyPath == null || bodyPath.isBreakingThrough()) {
            return;
        }

        double progressGain = damage * 3.3;
        double currentProgress = bodyPath.getCurrentRealmProgress();
        double newProgress = currentProgress - damage + progressGain;

        if (newProgress < 0.0) {
            newProgress = 0.0;
        }

        bodyPath.setCurrentRealmProgress(newProgress);
        bodyPath.sync(player);
    }

    @Override
    protected String getTitleKey() {
        return "ascension.skill.body_cultivation";
    }

    @Override
    protected String getDescriptionKey() {
        return "ascension.skill.body_cultivation.description";
    }

    @Override
    protected String getIconPath() {
        return "textures/spells/icon/placeholder.png";
    }
}
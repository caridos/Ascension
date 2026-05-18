package net.thejadeproject.ascension.refactor_packages.skills.custom.cultivation.weapon;

import net.lucent.easygui.gui.RenderableElement;
import net.lucent.easygui.gui.UIFrame;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.thejadeproject.ascension.data_attachments.ModAttachments;
import net.thejadeproject.ascension.refactor_packages.entity_data.IEntityData;
import net.thejadeproject.ascension.refactor_packages.gui.elements.info_elements.DescriptionDisplayContainer;
import net.thejadeproject.ascension.refactor_packages.paths.ModPaths;
import net.thejadeproject.ascension.refactor_packages.paths.data.IPathData;
import net.thejadeproject.ascension.refactor_packages.qi.EntityQiContainer;
import net.thejadeproject.ascension.refactor_packages.skills.custom.passive.SimplePassiveSkill;
import net.thejadeproject.ascension.refactor_packages.techniques.ITechnique;
import net.thejadeproject.ascension.util.ModTags;

public class FistCultivationSkill extends SimplePassiveSkill{

    private static final float MIN_DAMAGE = 2.0f;
    private static final double BASE_MULTIPLIER = 2.5D;
    private static final double QI_COST_MULTIPLIER = 1.0D;

    private final String titleKey;
    private final String descriptionKey;
    private final ResourceLocation skillId;

    public FistCultivationSkill(String titleKey, String descriptionKey, ResourceLocation skillId) {
        this.titleKey = titleKey;
        this.descriptionKey = descriptionKey;
        this.skillId = skillId;

        NeoForge.EVENT_BUS.addListener(this::onLivingDamage);
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;

        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        // Check if the damage came from a melee hit
        Entity directEntity = event.getSource().getDirectEntity();
        boolean isMelee = (directEntity == player);

        if (!isMelee) return;

        float damage = event.getNewDamage();
        if (damage < MIN_DAMAGE) return;

        ItemStack mainHand = player.getMainHandItem();

        boolean isFistWeapon = mainHand.isEmpty() || mainHand.is(ModTags.Items.FIST);
        if (!isFistWeapon) return;

        IEntityData entityData = player.getData(ModAttachments.ENTITY_DATA);
        if (entityData == null) return;
        if (!entityData.hasSkill(skillId)) return;

        IPathData fistPath = entityData.getPathData(ModPaths.FIST.getId());
        if (fistPath == null || fistPath.isBreakingThrough()) return;

        EntityQiContainer qiContainer = entityData.getQiContainer();
        if (qiContainer == null) return;

        double qiCost = damage * QI_COST_MULTIPLIER;
        if (!qiContainer.hasQi(qiCost)) return;
        if (!qiContainer.tryConsumeQi(qiCost)) return;

        double fistBonus = Math.max(
                1.0D,
                entityData.getPathBonusHandler().getPathBonus(ModPaths.FIST.getId())
        );

        double gain = damage * BASE_MULTIPLIER * fistBonus;

        ITechnique technique = fistPath.getCurrentTechnique();

        if (technique != null && fistPath.getCurrentRealmProgress() + gain >= technique.getMaxQiForRealm(
                fistPath.getMajorRealm(),
                fistPath.getMinorRealm()
        )) {
            fistPath.setCurrentRealmProgress(
                    technique.getMaxQiForRealm(
                            fistPath.getMajorRealm(),
                            fistPath.getMinorRealm()
                    )
            );

            if (fistPath.getMinorRealm() < technique.getMaxMinorRealm(fistPath.getMajorRealm())
                    && technique.canBreakthroughMinorRealm(
                    entityData,
                    fistPath.getMajorRealm(),
                    fistPath.getMinorRealm(),
                    fistPath.getCurrentRealmProgress()
            )) {
                fistPath.handleRealmChange(
                        fistPath.getMajorRealm(),
                        fistPath.getMinorRealm() + 1,
                        entityData
                );
            } else if (fistPath.getMajorRealm() < technique.getMaxMajorRealm()
                    && technique.canBreakthrough(
                    entityData,
                    fistPath.getMajorRealm(),
                    fistPath.getMinorRealm(),
                    fistPath.getCurrentRealmProgress()
            )) {
                // Trigger major breakthrough
                fistPath.handleRealmChange(fistPath.getMajorRealm() + 1, 0, entityData);
            }
        } else {
            fistPath.setCurrentRealmProgress(fistPath.getCurrentRealmProgress() + gain);
        }

        fistPath.sync(player);
    }

    @Override
    protected String getTitleKey() { return titleKey; }

    @Override
    protected String getDescriptionKey() { return descriptionKey; }

    @Override
    protected String getIconPath() { return "textures/spells/fist_aura.png"; }

    @OnlyIn(Dist.CLIENT)
    @Override
    public RenderableElement getInformationContainer(UIFrame frame, IEntityData entityData) {
        return new DescriptionDisplayContainer(frame,
                getTitle(entityData),
                getDescription(entityData));
    }
}

package net.thejadeproject.ascension.refactor_packages.skills.custom.passive.weapon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.thejadeproject.ascension.refactor_packages.paths.ModPaths;
import net.thejadeproject.ascension.util.ModTags;

public class FistMasterySkill extends GenericWeaponMasterySkill{
    @Override
    protected ResourceLocation getPathId() {
        return ModPaths.FIST.getId();
    }

    @Override
    protected TagKey<Item> getWeaponTag() {
        return ModTags.Items.FIST;
    }

    @Override
    public boolean matchesWeapon(ItemStack stack) {
        return stack.isEmpty() || (getWeaponTag() != null && stack.is(getWeaponTag()));
    }

    @Override
    protected String getVfxType() {
        return "";
    }

    @Override
    public boolean matchesDamage(ServerPlayer player, DamageSource source) {
        return super.matchesDamage(player, source) && source.getDirectEntity() == player;
    }

    @Override
    protected String getTitleKey() {
        return "ascension.skill.fist_mastery_skill";
    }

    @Override
    protected String getDescriptionKey() {
        return "ascension.skill.fist_mastery.description_skill";
    }

    @Override
    protected String getIconPath() { return "textures/spells/fist_aura.png"; }
}

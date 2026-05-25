package net.thejadeproject.ascension.util.ToolTips;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.thejadeproject.ascension.common.items.data_components.ModDataComponents;
import net.thejadeproject.ascension.common.items.tools.soul_weapon.SoulWeaponHelper;
import net.thejadeproject.ascension.data_attachments.ModAttachments;
import net.thejadeproject.ascension.refactor_packages.entity_data.IEntityData;
import net.thejadeproject.ascension.refactor_packages.paths.ModPaths;
import net.thejadeproject.ascension.refactor_packages.paths.data.IPathData;

public final class SoulWeaponTooltip {
    private SoulWeaponTooltip() {}

    public static void addTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();

        int soulMajor = 0;
        int soulMinor = 0;

        if (player != null && player.hasData(ModAttachments.ENTITY_DATA)) {
            IEntityData entityData = player.getData(ModAttachments.ENTITY_DATA);
            IPathData soulPath = entityData.getPathData(ModPaths.SOUL.getId());

            if (soulPath != null) {
                soulMajor = soulPath.getMajorRealm();
                soulMinor = soulPath.getMinorRealm();
            }
        }


        var soulWeapon = event.getItemStack().get(ModDataComponents.SOUL_WEAPON.get());

        if (soulWeapon == null) {
            return;
        }

        int grade = soulWeapon.grade();
        int forgedMarks = soulWeapon.forgedMarks();
        String type = soulWeapon.type();


        float damage = SoulWeaponHelper.calculateSoulWeaponDamage(
                soulMajor,
                soulMinor,
                grade,
                forgedMarks,
                type
        );

        event.getToolTip().add(Component.translatable(
                "item.ascension.soulbound_weapon.damage",
                String.format("%.1f", damage)
        ).withStyle(ChatFormatting.DARK_AQUA));
    }
}
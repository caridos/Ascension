package net.thejadeproject.ascension.refactor_packages.handlers.player;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.thejadeproject.ascension.AscensionCraft;
import net.thejadeproject.ascension.common.items.data_components.ModDataComponents;
import net.thejadeproject.ascension.common.items.tools.soul_weapon.SoulWeaponHelper;
import net.thejadeproject.ascension.data_attachments.ModAttachments;
import net.thejadeproject.ascension.data_attachments.attachments.SoulWeaponData;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.thejadeproject.ascension.refactor_packages.entity_data.IEntityData;
import net.thejadeproject.ascension.refactor_packages.paths.ModPaths;
import net.thejadeproject.ascension.refactor_packages.paths.data.IPathData;

@EventBusSubscriber(modid = AscensionCraft.MOD_ID)
public final class SoulWeaponHandler {
    private SoulWeaponHandler() {}

    @SubscribeEvent
    public static void onSoulWeaponDropped(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getEntity().getItem();

        if (!SoulWeaponHelper.isSoulWeapon(stack)) return;
        if (!SoulWeaponHelper.isOwner(stack, player)) return;

        event.setCanceled(true);
        event.getEntity().discard();

        SoulWeaponData data = player.getData(ModAttachments.SOUL_WEAPON);
        data.summoned = false;
    }

    @SubscribeEvent
    public static void onSoulWeaponOwnerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SoulWeaponHelper.removeOwnedSoulWeapons(player);

        SoulWeaponData data = player.getData(ModAttachments.SOUL_WEAPON);
        data.summoned = false;
    }

    @SubscribeEvent
    public static void onSoulWeaponKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ItemStack weapon = player.getMainHandItem();

        if (!SoulWeaponHelper.isSoulWeapon(weapon)) return;
        if (!SoulWeaponHelper.isOwner(weapon, player)) return;

        SoulWeaponData data = player.getData(ModAttachments.SOUL_WEAPON);

        if (!data.bound || !data.summoned) return;

        int progressGain = SoulWeaponHelper.getTemperingGain(event.getEntity());
        data.currentTempering += progressGain;

        boolean gradedUp = false;

        while (data.currentTempering >= SoulWeaponHelper.getRequiredTempering(data.currentGrade, data.lifetimeMarks)) {
            data.currentTempering -= SoulWeaponHelper.getRequiredTempering(data.currentGrade, data.lifetimeMarks);
            data.currentGrade++;
            data.lifetimeMarks++;
            gradedUp = true;
        }

        SoulWeaponHelper.writeSoulWeaponComponent(weapon, player, data);

        if (gradedUp) {
            player.displayClientMessage(
                    Component.translatable("item.ascension.soulbound_weapon.grade_advanced", data.currentGrade),
                    true
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        SoulWeaponData data = player.getData(ModAttachments.SOUL_WEAPON);

        if (!player.hasData(ModAttachments.ENTITY_DATA)) return;
        IEntityData entityData = player.getData(ModAttachments.ENTITY_DATA);
        IPathData soulPath = entityData.getPathData(ModPaths.SOUL.getId());

        int soulMajor = soulPath != null ? soulPath.getMajorRealm() : 0;
        int soulMinor = soulPath != null ? soulPath.getMinorRealm() : 0;

        SoulWeaponHelper.syncSoulRealmProgress(data, soulMajor, soulMinor);

        int ownedSoulWeapons = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (!SoulWeaponHelper.isSoulWeapon(stack)) continue;

            if (!SoulWeaponHelper.isOwner(stack, player)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                continue;
            }

            ownedSoulWeapons++;

            if (!data.bound || !data.summoned || ownedSoulWeapons > 1) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                continue;
            }

            SoulWeaponHelper.writeSoulWeaponComponent(stack, player, data);

        }

        if (ownedSoulWeapons == 0 && data.summoned) {
            data.summoned = false;
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SoulWeaponData data = player.getData(ModAttachments.SOUL_WEAPON);

        for (int slotIndex = 0; slotIndex < event.getContainer().slots.size(); slotIndex++) {
            var slot = event.getContainer().slots.get(slotIndex);

            if (slot.container instanceof Inventory) continue;

            ItemStack stack = slot.getItem();

            if (!SoulWeaponHelper.isSoulWeapon(stack)) continue;
            if (!SoulWeaponHelper.isOwner(stack, player)) continue;

            slot.set(ItemStack.EMPTY);
            data.summoned = false;

            player.displayClientMessage(
                    Component.translatable("ascension.skill.soul_forge.returned_from_container"),
                    true
            );
        }
    }

    @SubscribeEvent
    public static void onSoulWeaponDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ItemStack weapon = player.getMainHandItem();

        if (!SoulWeaponHelper.isSoulWeapon(weapon)) return;
        if (!SoulWeaponHelper.isOwner(weapon, player)) return;
        if (!player.hasData(ModAttachments.ENTITY_DATA)) return;


        IEntityData entityData = player.getData(ModAttachments.ENTITY_DATA);
        IPathData soulPath = entityData.getPathData(ModPaths.SOUL.getId());

        int soulMajor = soulPath != null ? soulPath.getMajorRealm() : 0;
        int soulMinor = soulPath != null ? soulPath.getMinorRealm() : 0;

        var soulWeapon = weapon.get(ModDataComponents.SOUL_WEAPON.get());
        if (soulWeapon == null) return;

        float bonusDamage = SoulWeaponHelper.calculateSoulWeaponBonus(
                soulMajor,
                soulMinor,
                soulWeapon.grade(),
                soulWeapon.forgedMarks(),
                soulWeapon.type()
        );

        event.setNewDamage(event.getNewDamage() + bonusDamage);
    }

}
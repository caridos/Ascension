package net.thejadeproject.ascension.common.items.techniques;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.thejadeproject.ascension.data_attachments.ModAttachments;
import net.thejadeproject.ascension.common.items.data_components.ModDataComponents;
import net.thejadeproject.ascension.common.items.ModItems;
import net.thejadeproject.ascension.refactor_packages.network.client_bound.toast.ShowAscensionToast;
import net.thejadeproject.ascension.refactor_packages.registries.AscensionRegistries;
import net.thejadeproject.ascension.refactor_packages.techniques.ITechnique;
import net.thejadeproject.ascension.refactor_packages.techniques.merge.TechniqueMergeHandler;

public class TechniqueTransferItem extends Item {

    public TechniqueTransferItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);

        String techniqueId = stack.get(ModDataComponents.TECHNIQUE_ID.get());
        if (techniqueId == null) return InteractionResultHolder.fail(stack);

        ResourceLocation techResLoc = ResourceLocation.parse(techniqueId);

        if (player.isShiftKeyDown()) {
            // Shift+right-click: learn the technique directly
            Component techniqueName = getName(stack);
            ItemStack toastIcon = stack.copy();
            toastIcon.setCount(1);

            if (player.getData(ModAttachments.ENTITY_DATA).setTechnique(techResLoc)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer,
                        new ShowAscensionToast(techniqueName.getString(), "Technique Learned", toastIcon));
                }
            } else {
                player.sendSystemMessage(Component.literal("Unable to learn ").append(techniqueName).append("!"));
            }
            return InteractionResultHolder.success(stack);
        }

        // Right-click: attempt merge with existing technique history
        ResourceLocation mergeResult = TechniqueMergeHandler.findMergeResult(
            player.getData(ModAttachments.ENTITY_DATA), techResLoc);

        if (mergeResult == null) {
            int blockedSize = TechniqueMergeHandler.findBlockedComboSize(
                player.getData(ModAttachments.ENTITY_DATA), techResLoc);
            if (blockedSize == 5) {
                player.sendSystemMessage(Component.literal("Requires realm 4, stage 9 to perform this merge."));
            } else if (blockedSize > 0) {
                player.sendSystemMessage(Component.literal("Requires realm " + blockedSize + " to perform this merge."));
            } else {
                player.sendSystemMessage(Component.literal("No compatible techniques to merge with."));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);

        TechniqueMergeHandler.applyMerge(serverPlayer, mergeResult);

        ITechnique merged = AscensionRegistries.Techniques.TECHNIQUES_REGISTRY.get(mergeResult);
        Component mergedName = merged != null ? merged.getDisplayTitle() : Component.literal("Unknown");

        if (!player.getAbilities().instabuild) stack.shrink(1);

        PacketDistributor.sendToPlayer(serverPlayer,
            new ShowAscensionToast(mergedName.getString(), "Techniques Merged", stack));

        return InteractionResultHolder.success(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        String targetTechnique = stack.get(ModDataComponents.TECHNIQUE_ID.get());

        if (targetTechnique != null) {
            ResourceLocation techniqueId = ResourceLocation.parse(targetTechnique);
            ITechnique technique = AscensionRegistries.Techniques.TECHNIQUES_REGISTRY.get(techniqueId);

            if (technique != null) return technique.getDisplayTitle();
        }

        return Component.literal("Manual");
    }

    public static ItemStack createWithTechnique(String techniqueId) {
        ItemStack stack = new ItemStack(ModItems.TECHNIQUE_MANUAL.get());
        stack.set(ModDataComponents.TECHNIQUE_ID.get(), techniqueId);
        return stack;
    }
}
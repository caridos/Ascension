package net.thejadeproject.ascension.refactor_packages.handlers.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.thejadeproject.ascension.AscensionCraft;
import net.thejadeproject.ascension.data_attachments.ModAttachments;
import net.thejadeproject.ascension.data_attachments.attachments.PlayerInputStates;
import net.thejadeproject.ascension.refactor_packages.entity_data.GenericEntityData;
import net.thejadeproject.ascension.refactor_packages.entity_data.IEntityData;
import net.thejadeproject.ascension.refactor_packages.forms.IEntityFormData;
import net.thejadeproject.ascension.refactor_packages.network.client_bound.entity_data.attributes.SyncAttributeHolder;
import net.thejadeproject.ascension.refactor_packages.paths.PathData;

@EventBusSubscriber(modid = AscensionCraft.MOD_ID)
public class ServerPlayerEventHandlers {




    @SubscribeEvent
    public static void onInputStateChanged(PlayerInputStates.InputStateChanged inputStateChanged){
        if(inputStateChanged.input.equals("skill_cast") && inputStateChanged.state == PlayerInputStates.InputState.PRESSED){
            IEntityData entityData = inputStateChanged.player.getData(ModAttachments.ENTITY_DATA);
            //System.out.println("trying to cast active slot: "+entityData.getSkillCastHandler().getHotBar().getActiveSlot());
            entityData.getSkillCastHandler().tryCast(inputStateChanged.player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event){
        for(ServerPlayer player : event.getServer().getPlayerList().getPlayers()){
            player.getData(ModAttachments.ENTITY_DATA).getSkillCastHandler().tick(player);
            for(PathData pathData:player.getData(ModAttachments.ENTITY_DATA).getAllPathData()){
                if(pathData.isBreakingThrough() && pathData.getBreakthroughInstance() != null) pathData.getBreakthroughInstance().tick(player.getData(ModAttachments.ENTITY_DATA),pathData.getPath());
            }
        }
    }
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event){
        Player player = event.getEntity();
        player.getData(ModAttachments.ENTITY_DATA).setHealth(player.getData(ModAttachments.ENTITY_DATA).getAscensionAttributeHolder().getAttribute(Attributes.MAX_HEALTH).getValue());
        for(PathData pathData:player.getData(ModAttachments.ENTITY_DATA).getAllPathData()){
            if(pathData.isBreakingThrough() && pathData.getBreakthroughInstance() != null) pathData.getBreakthroughInstance().onEntityDeath(player.getData(ModAttachments.ENTITY_DATA),pathData.getPath());
        }
        if(!event.getEntity().level().isClientSide()){
            if(player.getData(ModAttachments.ENTITY_DATA) instanceof GenericEntityData genericEntityData){
                genericEntityData.sync(player);
                genericEntityData.getAscensionAttributeHolder().log();
            }
            player.getData(ModAttachments.ENTITY_DATA).getSkillCastHandler().sync(player);
            PacketDistributor.sendToPlayer((ServerPlayer) player,new SyncAttributeHolder(player.getData(ModAttachments.ENTITY_DATA).getAscensionAttributeHolder()));

            for(IEntityFormData formData:player.getData(ModAttachments.ENTITY_DATA).getFormData()){
                formData.getStatSheet().sync((ServerPlayer) player,formData.getEntityFormId());
            }
        }

    }
}

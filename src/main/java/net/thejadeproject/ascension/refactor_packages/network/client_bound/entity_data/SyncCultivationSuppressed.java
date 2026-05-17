package net.thejadeproject.ascension.refactor_packages.network.client_bound.entity_data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.thejadeproject.ascension.AscensionCraft;
import net.thejadeproject.ascension.data_attachments.ModAttachments;
import net.thejadeproject.ascension.refactor_packages.entity_data.IEntityData;
import net.thejadeproject.ascension.refactor_packages.network.client_bound.entity_data.stats.SyncStat;
import net.thejadeproject.ascension.refactor_packages.util.value_modifiers.ValueContainer;

public record SyncCultivationSuppressed(boolean state) implements CustomPacketPayload {

    public static final Type<SyncCultivationSuppressed> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AscensionCraft.MOD_ID,"sync_cultivation_suppressed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCultivationSuppressed> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            SyncCultivationSuppressed::state,
            SyncCultivationSuppressed::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handlePayload(SyncCultivationSuppressed payload, IPayloadContext context) {
        context.enqueueWork(()->{
            IEntityData entityData = context.player().getData(ModAttachments.ENTITY_DATA);
            entityData.setSuppressed(payload.state);
        });
    }
}

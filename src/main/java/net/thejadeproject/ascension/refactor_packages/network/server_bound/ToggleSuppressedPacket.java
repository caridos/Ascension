package net.thejadeproject.ascension.refactor_packages.network.server_bound;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.thejadeproject.ascension.AscensionCraft;
import net.thejadeproject.ascension.data_attachments.ModAttachments;

public record ToggleSuppressedPacket(boolean newState) implements CustomPacketPayload {

    public static final Type<ToggleSuppressedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AscensionCraft.MOD_ID, "toggle_suppressed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleSuppressedPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    ToggleSuppressedPacket::newState,
                    ToggleSuppressedPacket::new
            );


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handlePayload(ToggleSuppressedPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            serverPlayer.getData(ModAttachments.ENTITY_DATA).setSuppressed(payload.newState);
        });
    }
}

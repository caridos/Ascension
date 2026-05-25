package net.thejadeproject.ascension.common.items.tools.data.soul_weapon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record SoulWeaponComponent(
        UUID owner,
        String type,
        int grade,
        int tempering,
        int forgedMarks
) {
    public static final Codec<SoulWeaponComponent> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(SoulWeaponComponent::owner),
            Codec.STRING.fieldOf("type").forGetter(SoulWeaponComponent::type),
            Codec.INT.fieldOf("currentGrade").forGetter(SoulWeaponComponent::grade),
            Codec.INT.fieldOf("tempering").forGetter(SoulWeaponComponent::tempering),
            Codec.INT.fieldOf("forged_marks").forGetter(SoulWeaponComponent::forgedMarks)
    ).apply(inst, SoulWeaponComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulWeaponComponent> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, SoulWeaponComponent::owner,
                    ByteBufCodecs.STRING_UTF8, SoulWeaponComponent::type,
                    ByteBufCodecs.VAR_INT, SoulWeaponComponent::grade,
                    ByteBufCodecs.VAR_INT, SoulWeaponComponent::tempering,
                    ByteBufCodecs.VAR_INT, SoulWeaponComponent::forgedMarks,
                    SoulWeaponComponent::new
            );
}
package net.thejadeproject.ascension.common.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.thejadeproject.ascension.data_attachments.ModAttachments;
import net.thejadeproject.ascension.mob_cultivation.*;
import net.thejadeproject.ascension.mob_cultivation.events.MobCultivationEvents;
import net.thejadeproject.ascension.mob_cultivation.util.MobCultivationCommandHelper;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public final class MobCultivationCommand {
    private MobCultivationCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("mob")

                // ------------------------------------------------------------------
                // /ascension mob summon <entity> <realm> <stage> [pos]
                // Summons a mob at a specific cultivation rank.
                // ------------------------------------------------------------------
                .then(Commands.literal("summon")
                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                .suggests((context, builder) -> {
                                    BuiltInRegistries.ENTITY_TYPE.keySet()
                                            .forEach(id -> builder.suggest(id.toString()));
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("realm", word())
                                        .suggests((context, builder) -> {
                                            MobCultivationList.getRealmIds()
                                                    .forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("stage", integer(1, 3))
                                                .executes(ctx -> summon(
                                                        ctx.getSource(),
                                                        ResourceLocationArgument.getId(ctx, "entity"),
                                                        getString(ctx, "realm"),
                                                        getInteger(ctx, "stage"),
                                                        ctx.getSource().getPosition()
                                                ))
                                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                                        .executes(ctx -> summon(
                                                                ctx.getSource(),
                                                                ResourceLocationArgument.getId(ctx, "entity"),
                                                                getString(ctx, "realm"),
                                                                getInteger(ctx, "stage"),
                                                                Vec3Argument.getVec3(ctx, "pos")
                                                        ))
                                                )
                                        )
                                )
                        )
                )

                // ------------------------------------------------------------------
                // /ascension mob set <target> <realm> <stage>
                // Sets the cultivation rank of an existing entity.
                // ------------------------------------------------------------------
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("realm", word())
                                        .suggests((context, builder) -> {
                                            MobCultivationList.getRealmIds()
                                                    .forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("stage", integer(1, 3))
                                                .executes(ctx -> set(
                                                        ctx.getSource(),
                                                        EntityArgument.getEntity(ctx, "target"),
                                                        getString(ctx, "realm"),
                                                        getInteger(ctx, "stage")
                                                ))
                                        )
                                )
                        )
                )

                // ------------------------------------------------------------------
                // /ascension mob stats <target>
                // Prints full cultivation and attribute stats for an entity.
                // ------------------------------------------------------------------
                .then(Commands.literal("stats")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> getStats(
                                        ctx.getSource(),
                                        EntityArgument.getEntity(ctx, "target")
                                ))
                        )
                )

                // ------------------------------------------------------------------
                // /ascension mob forcespawn <entity> [pos]
                // Forces an above-mortal elite mob to spawn using the same logic as
                // the natural elite spawn path, ignoring the cooldown. The rank is
                // resolved relative to the nearest non-creative/spectator player.
                // ------------------------------------------------------------------
                .then(Commands.literal("forcespawn")
                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                .suggests((context, builder) -> {
                                    BuiltInRegistries.ENTITY_TYPE.keySet()
                                            .forEach(id -> builder.suggest(id.toString()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> forceSpawn(
                                        ctx.getSource(),
                                        ResourceLocationArgument.getId(ctx, "entity"),
                                        ctx.getSource().getPosition()
                                ))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(ctx -> forceSpawn(
                                                ctx.getSource(),
                                                ResourceLocationArgument.getId(ctx, "entity"),
                                                Vec3Argument.getVec3(ctx, "pos")
                                        ))
                                )
                        )
                );
    }

    // -------------------------------------------------------------------------
    // Command implementations
    // -------------------------------------------------------------------------

    private static int summon(
            CommandSourceStack source,
            ResourceLocation entityId,
            String realmId,
            int stage,
            Vec3 pos
    ) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (type == null) {
            source.sendFailure(Component.literal("Unknown entity type: " + entityId));
            return 0;
        }

        Entity entity = type.create(source.getLevel());
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("Entity is not a LivingEntity: " + entityId));
            return 0;
        }

        living.moveTo(pos.x, pos.y, pos.z, source.getRotation().y, 0.0F);

        if (!MobCultivationCommandHelper.applyCultivation(living, realmId, stage)) {
            source.sendFailure(Component.literal("Failed to apply mob cultivation."));
            return 0;
        }

        source.getLevel().addFreshEntity(living);
        source.sendSuccess(
                () -> Component.literal(
                        "Summoned " + entityId + " with " + realmId + " stage " + stage),
                true
        );
        return 1;
    }

    private static int set(CommandSourceStack source, Entity entity, String realmId, int stage) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("Target is not a LivingEntity."));
            return 0;
        }

        if (!MobCultivationCommandHelper.applyCultivation(living, realmId, stage)) {
            source.sendFailure(Component.literal("Failed to apply mob cultivation."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Set " + living.getName().getString() + " to " + realmId + " stage " + stage),
                true
        );
        return 1;
    }

    private static int getStats(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("Target is not a LivingEntity."));
            return 0;
        }

        source.sendSuccess(() -> MobCultivationCommandHelper.getStatsMessage(living), false);
        return 1;
    }

    /**
     * Spawns an entity, resolves its rank relative to the nearest player using
     * the same elite-rank logic as natural spawns, then broadcasts the notification.
     * Bypasses the cooldown — this is a manual command.
     */
    private static int forceSpawn(
            CommandSourceStack source,
            ResourceLocation entityId,
            Vec3 pos
    ) {
        ServerLevel level = source.getLevel();

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (type == null) {
            source.sendFailure(Component.literal("Unknown entity type: " + entityId));
            return 0;
        }

        Entity entity = type.create(level);
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("Entity is not a LivingEntity: " + entityId));
            return 0;
        }

        if (!MobCultivationRoller.canHaveRank(living)) {
            source.sendFailure(Component.literal(entityId + " cannot have a cultivation rank."));
            return 0;
        }

        living.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);

        // Resolve rank. resolveAroundNearbyPlayer searches for a nearby player
        // itself, so we just call it directly. Falls back to mortal 1 if no
        // player is close enough.
        MobCultivationDefinition definition =
                MobCultivationResolver.resolveAroundNearbyPlayer(living);

        if (definition == null) {
            // No nearby player — use nearest player in level as reference instead.
            Player nearest = level.getNearestPlayer(pos.x, pos.y, pos.z, -1, false);
            if (nearest != null) {
                definition = MobCultivationResolver.resolveFromPlayer(nearest);
            } else {
                definition = MobCultivationList.getFirst();
            }
        }

        MobCultivationData data = living.getData(ModAttachments.MOB_RANK);
        if (data == null) {
            source.sendFailure(Component.literal("Entity has no cultivation data attachment."));
            return 0;
        }

        MobCultivationEvents.assignRank(living, data, definition);
        level.addFreshEntity(living);

        // Send the same notification as a natural elite spawn, but only if
        // the rank is actually above mortal 1.
        int power = MobCultivationResolver.getRankPower(
                definition.realmId(), definition.stage());
        if (power > 0) {
            MobCultivationEvents.sendEliteSpawnNotification(level, living, definition);
        }

        final MobCultivationDefinition finalDef = definition;
        source.sendSuccess(
                () -> Component.literal(
                        "Force-spawned " + entityId
                                + " at [" + (int)pos.x + " " + (int)pos.y + " " + (int)pos.z + "]"
                                + " as " + finalDef.realmId() + " stage " + finalDef.stage()),
                true
        );
        return 1;
    }
}
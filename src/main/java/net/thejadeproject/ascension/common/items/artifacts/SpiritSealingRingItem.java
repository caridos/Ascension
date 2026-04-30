package net.thejadeproject.ascension.common.items.artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.thejadeproject.ascension.common.items.data_components.ModDataComponents;
import net.thejadeproject.ascension.events.SealedEntityData;
import net.thejadeproject.ascension.util.ModTags;

import java.util.List;
import java.util.Optional;

public class SpiritSealingRingItem extends Item {

    // Sealing duration in ticks (3 seconds)
    public static final int SEALING_DURATION = 60;

    public SpiritSealingRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();

        // Cannot seal if already has entity
        if (hasEntityStored(stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("The ring already contains a sealed spirit!").withStyle(ChatFormatting.RED),
                        true
                );
            }
            return InteractionResult.FAIL;
        }

        // Check blacklist
        if (target.getType().is(ModTags.EntityTypes.SEALING_BLACKLIST)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("This entity cannot be sealed by mortal means.").withStyle(ChatFormatting.DARK_RED),
                        true
                );
                level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS, 0.5f, 0.5f);
            }
            return InteractionResult.FAIL;
        }

        // Boss check
        if (target.getType().is(ModTags.EntityTypes.BOSS)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("The spirit is too powerful to seal!").withStyle(ChatFormatting.RED),
                        true
                );
            }
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            // Remove item from hand and start sealing process
            ItemStack sealingStack = stack.copy();
            player.setItemInHand(hand, ItemStack.EMPTY);

            // Start sealing animation using ItemEntity
            startSealingProcess((ServerLevel) level, player, target, sealingStack, hand);
        }

        return InteractionResult.SUCCESS;
    }

    private void startSealingProcess(ServerLevel level, Player player, LivingEntity target, ItemStack ringStack, InteractionHand hand) {
        // Create item entity that floats above target with custom behavior
        Vec3 spawnPos = target.position().add(0, target.getBbHeight() + 2.5, 0);

        ItemEntity itemEntity = new ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, ringStack) {
            private int life = 0;
            private final int MAX_LIFE = SEALING_DURATION;
            private final int targetId = target.getId(); // Store ID, not UUID
            private boolean interrupted = false;

            @Override
            public void tick() {
                life++;

                // Get target reference by ID
                Entity entity = level().getEntity(targetId);
                LivingEntity currentTarget = null;
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    currentTarget = living;
                }

                // Float above target
                if (currentTarget != null) {
                    double bob = Math.sin(life * 0.2) * 0.2;
                    Vec3 targetPos = currentTarget.position().add(0, currentTarget.getBbHeight() + 2.5 + bob, 0);
                    this.setPos(targetPos.x, targetPos.y, targetPos.z);
                    this.setDeltaMovement(0, 0, 0);
                    this.setNoGravity(true);

                    // Spin
                    this.setYRot(life * 12);
                    this.setXRot(0);

                    // Spawn particles
                    spawnParticles(currentTarget, life);
                }

                // Check for completion
                if (life >= MAX_LIFE) {
                    if (currentTarget != null && currentTarget.isAlive() && currentTarget.hurtTime == 0) {
                        completeSealing(level, currentTarget, ringStack, player, hand);
                    } else {
                        interruptSealing(level, currentTarget, ringStack, this.position());
                    }
                    this.discard();
                    return;
                }

                // Check for interruption (damage or death)
                if (currentTarget == null || !currentTarget.isAlive() || currentTarget.hurtTime > 0) {
                    interruptSealing(level, currentTarget, ringStack, this.position());
                    this.discard();
                    return;
                }

                // Don't call super.tick() to avoid normal item physics
            }

            private void spawnParticles(LivingEntity target, int tick) {
                if (!(level() instanceof ServerLevel serverLevel)) return;

                float progress = tick / (float) MAX_LIFE;
                double x = target.getX();
                double y = target.getY() + target.getBbHeight() / 2;
                double z = target.getZ();

                // Ring of particles around mob
                int particleCount = 12;
                float radius = 1.5f * (1.0f - progress * 0.3f);
                float angle = tick * 0.15f;

                for (int i = 0; i < particleCount; i++) {
                    double theta = (2 * Math.PI * i / particleCount) + angle;
                    double px = x + Math.cos(theta) * radius;
                    double pz = z + Math.sin(theta) * radius;
                    double py = y + Math.sin(tick * 0.1 + i) * 0.3;

                    if (progress < 0.5) {
                        serverLevel.sendParticles(ParticleTypes.WITCH, px, py, pz, 1, 0, 0.01, 0, 0);
                    } else {
                        serverLevel.sendParticles(ParticleTypes.PORTAL, px, py, pz, 1, 0, 0.01, 0, 0);
                    }
                }

                // Spiral particles retracting
                if (progress > 0.2f) {
                    int spirals = 3;
                    for (int s = 0; s < spirals; s++) {
                        double spiralAngle = (tick * 0.2) + (s * (2 * Math.PI / spirals));
                        double spiralProgress = (progress - 0.2f) / 0.8f;
                        double spiralRadius = radius * (1 - spiralProgress);
                        double sx = x + Math.cos(spiralAngle) * spiralRadius;
                        double sz = z + Math.sin(spiralAngle) * spiralRadius;
                        double sy = y + Math.sin(tick * 0.15 + s) * 0.5;

                        serverLevel.sendParticles(ParticleTypes.ENCHANT, sx, sy, sz, 1, 0, -0.02, 0, 0);
                    }
                }

                // Soul particles rising
                if (tick % 5 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            x + (Math.random() - 0.5) * 0.5,
                            y + (Math.random() - 0.5) * 0.5,
                            z + (Math.random() - 0.5) * 0.5,
                            1, 0, 0.05, 0, 0);
                }

                // End particles
                if (progress > 0.85) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                            x + (Math.random() - 0.5) * 2,
                            y + (Math.random() - 0.5) * 2,
                            z + (Math.random() - 0.5) * 2,
                            1, 0, 0, 0, 0);
                }

                // Particles from ring to mob
                if (tick % 4 == 0 && progress > 0.3) {
                    Vec3 ringPos = position();
                    Vec3 mobPos = target.position().add(0, target.getBbHeight() / 2, 0);
                    Vec3 direction = mobPos.subtract(ringPos).normalize().scale(0.15);

                    serverLevel.sendParticles(ParticleTypes.WITCH,
                            ringPos.x, ringPos.y, ringPos.z,
                            1, direction.x, direction.y, direction.z, 0.1);
                }
            }

            @Override
            public boolean isPickable() {
                return false;
            }

            @Override
            public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
                // Interrupt if item entity is hit
                Entity entity = level().getEntity(targetId);
                if (entity instanceof LivingEntity living) {
                    interruptSealing((ServerLevel) level(), living, ringStack, position());
                }
                this.discard();
                return true;
            }
        };

        itemEntity.setNoGravity(true);
        itemEntity.setPickUpDelay(9999);
        itemEntity.setGlowingTag(true); // Make it glow so it's visible

        level.addFreshEntity(itemEntity);

        // Freeze target
        target.setNoGravity(true);
        target.setDeltaMovement(0, 0, 0);
        target.setSilent(true);
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
        }

        // Play sound
        level.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.5f, 0.5f);
    }

    public static void completeSealing(ServerLevel level, LivingEntity target, ItemStack ringStack, Player player, InteractionHand hand) {
        // Unfreeze target before removing (prevents issues)
        target.setNoGravity(false);
        target.setSilent(false);
        if (target instanceof Mob mob) {
            mob.setNoAi(false);
        }

        // Capture entity data
        net.minecraft.nbt.CompoundTag entityData = new net.minecraft.nbt.CompoundTag();
        target.save(entityData);

        // Remove UUID to prevent conflicts when spawning later
        if (entityData.hasUUID("UUID")) {
            entityData.remove("UUID");
        }

        String customName = target.hasCustomName() ? target.getCustomName().getString() :
                Component.translatable(target.getType().getDescriptionId()).getString();

        SealedEntityData sealedData = new SealedEntityData(
                target.getType(),
                entityData,
                Optional.of(target.getUUID()),
                customName,
                level.getGameTime()
        );

        // Store in ring
        ringStack.set(ModDataComponents.SEALED_ENTITY.get(), sealedData);

        // Remove original entity
        target.discard();

        // Return ring to player or drop if inventory full
        if (!player.getInventory().add(ringStack)) {
            player.drop(ringStack, false);
        }

        // Success sound and particles
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 1.0f, 1.5f);
        level.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1, player.getZ(),
                30, 0.5, 0.5, 0.5, 0.1);

        // Success message
        player.displayClientMessage(
                Component.literal("Sealed " + customName + " into the Spirit Ring!").withStyle(ChatFormatting.GREEN),
                true
        );
    }

    public static void interruptSealing(ServerLevel level, LivingEntity target, ItemStack ringStack, Vec3 dropPos) {
        // Unfreeze target
        if (target != null && target.isAlive()) {
            target.setNoGravity(false);
            target.setSilent(false);
            if (target instanceof Mob mob) {
                mob.setNoAi(false);
            }
        }

        // Drop ring on ground
        ItemEntity droppedItem = new ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, ringStack);
        droppedItem.setDefaultPickUpDelay();
        level.addFreshEntity(droppedItem);

        // Failure effects
        level.playSound(null, BlockPos.containing(dropPos), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 0.8f, 0.5f);
        level.sendParticles(ParticleTypes.SMOKE,
                dropPos.x, dropPos.y, dropPos.z,
                20, 0.5, 0.5, 0.5, 0.1);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!hasEntityStored(stack)) {
            return InteractionResultHolder.pass(stack);
        }

        // Release entity on shift-right-click in air
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                releaseEntity((ServerLevel) level, player, stack, hand);
            }
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        net.minecraft.core.Direction face = context.getClickedFace();

        if (!hasEntityStored(stack)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide && player != null) {
            BlockPos spawnPos = pos.relative(face);
            releaseEntityAtPosition((ServerLevel) level, player, stack, spawnPos);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    private void releaseEntity(ServerLevel level, Player player, ItemStack stack, InteractionHand hand) {
        BlockPos spawnPos = player.blockPosition().relative(player.getDirection());
        releaseEntityAtPosition(level, player, stack, spawnPos);
    }

    private void releaseEntityAtPosition(ServerLevel level, Player player, ItemStack stack, BlockPos pos) {
        SealedEntityData data = stack.get(ModDataComponents.SEALED_ENTITY.get());
        if (data == null) return;

        Entity entity = data.entityType().create(level);
        if (entity == null) return;

        // Restore data
        entity.load(data.entityData());

        // Explicitly ensure AI is enabled (remove NoAI if present)
        if (entity instanceof Mob mob) {
            // Remove the NoAI tag if it was saved
            if (mob.getPersistentData().contains("NoAI")) {
                mob.getPersistentData().remove("NoAI");
            }
            // Force enable AI
            mob.setNoAi(false);
            mob.setPersistenceRequired();

            // Re-enable natural spawning behavior
            mob.setAggressive(false);
            mob.setTarget(null);
        }

        // Set new position
        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                player.getYRot(), 0);

        // Ensure no gravity issues
        entity.setNoGravity(false);
        entity.setSilent(false);

        level.addFreshEntity(entity);

        // Clear ring
        stack.remove(ModDataComponents.SEALED_ENTITY.get());

        // Effects
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.PORTAL,
                pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                30, 0.5, 0.5, 0.5, 0.1);

        player.displayClientMessage(
                Component.literal("Released " + data.customName() + " from the ring!").withStyle(ChatFormatting.AQUA),
                true
        );
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return 13;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return hasEntityStored(stack) ? 0xFF0000 : 0x00FF00; // Red when full, Green when empty
    }

    public static boolean hasEntityStored(ItemStack stack) {
        return stack.has(ModDataComponents.SEALED_ENTITY.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (hasEntityStored(stack)) {
            SealedEntityData data = stack.get(ModDataComponents.SEALED_ENTITY.get());
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("§4§l[SEALED ENTITY]").withStyle(ChatFormatting.DARK_RED));
            tooltipComponents.add(Component.literal("§7Spirit: §f" + data.customName()));
            tooltipComponents.add(Component.literal("§7Type: §f" + data.getEntityId().getPath()));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("§5The spirit stirs within the void...").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        } else {
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("§2§l[EMPTY]").withStyle(ChatFormatting.DARK_GREEN));
            tooltipComponents.add(Component.literal("§7Right-click a mob to seal it").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("§7Shift-right-click to release").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("§oThe ring hungers for spirits...").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasEntityStored(stack) || super.isFoil(stack);
    }
}
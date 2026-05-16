package net.thejadeproject.ascension.refactor_packages.events.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

public class EntitySwingEvent extends Event {

    private final LivingEntity entity;
    private final InteractionHand hand;
    public EntitySwingEvent(LivingEntity entity, InteractionHand hand) {
        this.entity = entity;
        this.hand = hand;
    }

    public LivingEntity getEntity(){
        return entity;
    }
    public InteractionHand getHand(){
        return hand;
    }
}

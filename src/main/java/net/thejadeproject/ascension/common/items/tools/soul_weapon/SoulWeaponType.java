package net.thejadeproject.ascension.common.items.tools.soul_weapon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.thejadeproject.ascension.refactor_packages.paths.ModPaths;
import net.thejadeproject.ascension.util.ModTags;

public enum SoulWeaponType {
    AXE("axe", ModTags.Items.SOULFORGE_AXES, ModPaths.AXE.getId(), 0.2F),
    BLADE("blade", ModTags.Items.SOULFORGE_BLADES, ModPaths.BLADE.getId(), 0.3F),
    SPEAR("spear", ModTags.Items.SOULFORGE_SPEARS, ModPaths.SPEAR.getId(), 0.4F),
    //BOW("bow", ModTags.Items.SOULFORGE_BOWS, ModPaths.BOW.getId(), 0.5F),
    //TRIDENT("trident", ModTags.Items.SOULFORGE_TRIDENTS, ModPaths.TRIDENT.getId(), 0.6F),
    MACE("mace", ModTags.Items.SOULFORGE_MACES, ModPaths.MACE.getId(), 0.7F),
    //SHIELD("shield", ModTags.Items.SOULFORGE_SHIELDS, ModPaths.SHIELD.getId(), 0.8F),
    SWORD("sword", ModTags.Items.SOULFORGE_SWORDS, ModPaths.SWORD.getId(), 0.1F);

    private final String id;
    private final TagKey<Item> tag;
    private final ResourceLocation path;
    private final float modelPredicate;

    SoulWeaponType(String id, TagKey<Item> tag, ResourceLocation path, float modelPredicate) {
        this.id = id;
        this.tag = tag;
        this.path = path;
        this.modelPredicate = modelPredicate;
    }

    public String id() {
        return id;
    }

    public ResourceLocation path() {
        return path;
    }

    public static SoulWeaponType fromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;

        for (SoulWeaponType type : values()) {
            if (stack.is(type.tag)) {
                return type;
            }
        }

        return null;
    }

    public static SoulWeaponType fromId(String id) {
        for (SoulWeaponType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }

        return null;
    }


    public float modelPredicate() {
        return modelPredicate;
    }
}
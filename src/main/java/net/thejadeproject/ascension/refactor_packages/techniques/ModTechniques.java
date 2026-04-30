package net.thejadeproject.ascension.refactor_packages.techniques;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.thejadeproject.ascension.AscensionCraft;
import net.thejadeproject.ascension.refactor_packages.paths.ModPaths;
import net.thejadeproject.ascension.refactor_packages.registries.AscensionRegistries;
import net.thejadeproject.ascension.refactor_packages.stats.custom.ModStats;
import net.thejadeproject.ascension.refactor_packages.techniques.custom.FiveElementCultivationTechnique;
import net.thejadeproject.ascension.refactor_packages.techniques.custom.GenericTechnique;
import net.thejadeproject.ascension.refactor_packages.techniques.custom.ScholarlySoulTechnique;
import net.thejadeproject.ascension.refactor_packages.techniques.custom.WhiteLightningTenStageTechnique;
import net.thejadeproject.ascension.refactor_packages.techniques.custom.elemental.*;
import net.thejadeproject.ascension.refactor_packages.techniques.custom.stat_change_handlers.BasicStatChangeHandler;
import net.thejadeproject.ascension.refactor_packages.util.value_modifiers.ModifierOperation;
import net.thejadeproject.ascension.refactor_packages.util.value_modifiers.ValueContainerModifier;

import java.util.Set;

public class ModTechniques {
    public static final DeferredRegister<ITechnique> TECHNIQUES =DeferredRegister.create(AscensionRegistries.Techniques.TECHNIQUES_REGISTRY, AscensionCraft.MOD_ID);

    public static final ResourceLocation test = ResourceLocation.fromNamespaceAndPath(AscensionCraft.MOD_ID,"test");
    public static BasicStatChangeHandler testHandler = new BasicStatChangeHandler()
            .addMinorRealmStatModifier(ModStats.VITALITY.getId(),new ValueContainerModifier(2, ModifierOperation.ADD_BASE, test))
            .addMajorRealmStatModifier(ModStats.VITALITY.getId(),new ValueContainerModifier(0.2,ModifierOperation.MULTIPLY_FINAL,test))
            .addMinorRealmStatModifier(ModStats.AGILITY.getId(),new ValueContainerModifier(5,ModifierOperation.ADD_BASE,test));

    // ──── ESSENCE TECHNIQUES ────────────────────────────────────────────
    public static final DeferredHolder<ITechnique, ? extends GenericTechnique> BASIC_CULTIVATION_TECHNIQUE = TECHNIQUES.register("basic_cultivation_technique",()->
            new GenericTechnique(ModPaths.ESSENCE.getId(),Component.translatable("ascension.technique.basic_cultivation_technique"),2.0, Set.of())
                    .setStatChangeHandler(testHandler));
    public static final DeferredHolder<ITechnique, ? extends GenericTechnique> ADVANCED_CULTIVATION_TECHNIQUE = TECHNIQUES.register("advanced_cultivation_technique",()->
            new GenericTechnique(ModPaths.ESSENCE.getId(),Component.translatable("ascension.technique.advanced_cultivation_technique"),10.0,Set.of())
                    .setStatChangeHandler(testHandler));


    // ──── ESSENCE-ELEMENTAL HYBRID TECHNIQUES ────────────────────────────────────────────
    public static final DeferredHolder<ITechnique, ? extends FireEssenceTechnique> FIRE_ESSENCE_TECHNIQUE =
            TECHNIQUES.register("fire_essence_technique",
                    () -> new FireEssenceTechnique(testHandler));
    public static final DeferredHolder<ITechnique, ? extends WaterEssenceTechnique> WATER_ESSENCE_TECHNIQUE =
            TECHNIQUES.register("water_essence_technique",
                    () -> new WaterEssenceTechnique(testHandler));
    public static final DeferredHolder<ITechnique, ? extends WoodEssenceTechnique> WOOD_ESSENCE_TECHNIQUE =
            TECHNIQUES.register("wood_essence_technique",
                    () -> new WoodEssenceTechnique(testHandler));
    public static final DeferredHolder<ITechnique, ? extends EarthEssenceTechnique> EARTH_ESSENCE_TECHNIQUE =
            TECHNIQUES.register("earth_essence_technique",
                    () -> new EarthEssenceTechnique(testHandler));
    public static final DeferredHolder<ITechnique, ? extends MetalEssenceTechnique> METAL_ESSENCE_TECHNIQUE =
            TECHNIQUES.register("metal_essence_technique",
                    () -> new MetalEssenceTechnique(testHandler));
    public static final DeferredHolder<ITechnique, ? extends LightningEssenceTechnique> LIGHTNING_ESSENCE_TECHNIQUE =
            TECHNIQUES.register("lightning_essence_technique",
                    () -> new LightningEssenceTechnique(testHandler));
    public static final DeferredHolder<ITechnique, ? extends WindEssenceTechnique> WIND_ESSENCE_TECHNIQUE =
            TECHNIQUES.register("wind_essence_technique",
                    () -> new WindEssenceTechnique(testHandler));

    public static final DeferredHolder<ITechnique,? extends FiveElementCultivationTechnique> FIVE_ELEMENT_CIRCULATION_METHOD =
            TECHNIQUES.register("five_element_cultivation_technique",
                    (()->new FiveElementCultivationTechnique(testHandler)));


    // ──── BODY TECHNIQUES ────────────────────────────────────────────
    public static final DeferredHolder<ITechnique, ? extends WhiteLightningTenStageTechnique> WHITE_LIGHTNING_TEN_STAGE_TECHNIQUE =
            TECHNIQUES.register("white_lightning_ten_stage_technique",
                    () -> new WhiteLightningTenStageTechnique(testHandler));


    // ──── SOUL TECHNIQUES ────────────────────────────────────────────
    public static final DeferredHolder<ITechnique, ? extends ScholarlySoulTechnique> SCHOLARLY_SOUL_TECHNIQUE =
            TECHNIQUES.register("scholarly_soul_technique",
                    () -> new ScholarlySoulTechnique(testHandler));


    // ──── WEAPON TECHNIQUES ────────────────────────────────────────────
    public static final DeferredHolder<ITechnique, ? extends GenericTechnique> SWORD_COMPREHENSION_TECHNIQUE = TECHNIQUES.register("sword_comprehension_technique",()->
            new GenericTechnique(ModPaths.SWORD.getId(),Component.translatable("ascension.technique.sword_comprehension_technique"),10.0,Set.of()));


    /*
    ──── TECHNIQUE IDEAS | Fill in as you please ────────────────────────────────────────────

        public static final DeferredHolder<ITechnique, ? extends IndestructibleVajraTechnique> INDESTRUCTIBLE_VAJRA_SCRIPTURE
        public static final DeferredHolder<ITechnique, ? extends LotusHeartTechnique> LOTUS_HEART_SUTRA
        public static final DeferredHolder<ITechnique, ? extends TripleSoulTechnique> TRIPLE_SOUL_LIFE_SUTRA

        public static final DeferredHolder<ITechnique, ? extends AbyssDwellerTechnique> ABYSS_DWELLERS_MANUAL
        public static final DeferredHolder<ITechnique, ? extends HellboundMarrowTechnique> HELLBOUND_MARROW_SCRIPTURE
        public static final DeferredHolder<ITechnique, ? extends NetherQiTechnique> NETHER_QI_DEVOURING_SCRIPTURE

        public static final DeferredHolder<ITechnique, ? extends MirageArrowTechnique> MIRAGE_ARROW_MANUAL
        public static final DeferredHolder<ITechnique, ? extends GreatWallTechnique> BASTION_WALL_TECHNIQUE
        public static final DeferredHolder<ITechnique, ? extends MortalNineSaberTechnique> NINE_BLADES_SABER
        public static final DeferredHolder<ITechnique, ? extends EdgeTemperingTechnique> EDGE_TEMPERING_METHOD
        public static final DeferredHolder<ITechnique, ? extends FallingLeafBladeTechnique> FALLING_LEAF_BLADE
        public static final DeferredHolder<ITechnique, ? extends >
        public static final DeferredHolder<ITechnique, ? extends >

        public static final DeferredHolder<ITechnique, ? extends >
        public static final DeferredHolder<ITechnique, ? extends >
        public static final DeferredHolder<ITechnique, ? extends >
        public static final DeferredHolder<ITechnique, ? extends >
        public static final DeferredHolder<ITechnique, ? extends >

        P.S. I don't rally mean to make this many individual classes to extend, maybe just a couple general ones?
        Unless someone wants to :)
        I'm just jotting down ideas - sortofsmart

     ──── TECHNIQUE IDEAS | Fill in as you please ────────────────────────────────────────────
    */


    public static void register(IEventBus modEventBus){
        TECHNIQUES.register(modEventBus);
    }
}

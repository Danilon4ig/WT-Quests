package com.danilon4ig.wt_quests;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = Wt_quests.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();

    static final ForgeConfigSpec COMMON_SPEC = COMMON_BUILDER.build();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BIOMES = SERVER_BUILDER
            .comment("Biome IDs to search for chest placement (e.g. minecraft:beach, minecraft:forest)")
            .defineListAllowEmpty("biomes", List.of("minecraft:beach"), o -> o instanceof String);

    private static final ForgeConfigSpec.IntValue SEARCH_RADIUS = SERVER_BUILDER
            .comment("Radius in blocks to search for the target biome")
            .defineInRange("searchRadius", 200, 1, 10000);

    private static final ForgeConfigSpec.IntValue FALLBACK_MIN_DISTANCE = SERVER_BUILDER
            .comment("Minimum distance from reference for fallback placement")
            .defineInRange("fallbackMinDistance", 150, 1, 10000);

    private static final ForgeConfigSpec.IntValue FALLBACK_MAX_DISTANCE = SERVER_BUILDER
            .comment("Maximum distance from reference for fallback placement")
            .defineInRange("fallbackMaxDistance", 200, 1, 10000);

    static final ForgeConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

    public static List<? extends String> biomes;
    public static int searchRadius;
    public static int fallbackMinDistance;
    public static int fallbackMaxDistance;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            biomes = BIOMES.get();
            searchRadius = SEARCH_RADIUS.get();
            fallbackMinDistance = FALLBACK_MIN_DISTANCE.get();
            fallbackMaxDistance = FALLBACK_MAX_DISTANCE.get();
        }
    }
}

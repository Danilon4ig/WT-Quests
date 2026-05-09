package com.danilon4ig.wt_quests.api;

import com.danilon4ig.wt_quests.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class QuestApi {
    private static final Random RANDOM = new Random();

    public static ItemStack spawnChestQuest(ServerLevel level) {
        return spawnChestQuest(level, level.getSharedSpawnPos());
    }

    public static ItemStack spawnChestQuest(ServerLevel level, BlockPos referencePos) {
        BlockPos chestPos = findBiomePosition(level, referencePos);
        if (chestPos == null) {
            chestPos = findFallbackPosition(level, referencePos);
        }

        chestPos = getSurfacePos(level, chestPos);
        placeChest(level, chestPos);
        return createTreasureMap(level, chestPos);
    }

    @Nullable
    private static BlockPos findBiomePosition(ServerLevel level, BlockPos refPos) {
        int radius = Config.searchRadius;
        List<? extends String> biomeIds = Config.biomes;

        for (int r = 0; r <= radius; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;

                    BlockPos candidate = refPos.offset(dx, 0, dz);
                    Holder<Biome> holder = level.getBiome(candidate);

                    for (String biomeId : biomeIds) {
                        ResourceLocation id = ResourceLocation.tryParse(biomeId);
                        if (id == null) continue;
                        if (holder.is(ResourceKey.create(Registries.BIOME, id))) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos findFallbackPosition(ServerLevel level, BlockPos refPos) {
        int minDist = Config.fallbackMinDistance;
        int maxDist = Config.fallbackMaxDistance;
        int range = maxDist - minDist;

        for (int i = 0; i < 20; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double dist = minDist + RANDOM.nextDouble() * range;

            int x = refPos.getX() + (int) (Math.cos(angle) * dist);
            int z = refPos.getZ() + (int) (Math.sin(angle) * dist);
            BlockPos candidate = new BlockPos(x, 0, z);

            if (level.getWorldBorder().isWithinBounds(candidate)) {
                return candidate;
            }
        }
        return refPos.offset(200, 0, 0);
    }

    private static BlockPos getSurfacePos(ServerLevel level, BlockPos pos) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    private static void placeChest(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
    }

    private static ItemStack createTreasureMap(ServerLevel level, BlockPos pos) {
        ItemStack stack = MapItem.create(level, pos.getX(), pos.getZ(), (byte) 2, true, true);
        MapItemSavedData mapData = MapItem.getSavedData(stack, level);
        if (mapData != null) {
            int centerX = ObfuscationReflectionHelper.getPrivateValue(MapItemSavedData.class, mapData, "centerX");
            int centerZ = ObfuscationReflectionHelper.getPrivateValue(MapItemSavedData.class, mapData, "centerZ");
            byte scale = ObfuscationReflectionHelper.getPrivateValue(MapItemSavedData.class, mapData, "scale");
            Map<String, MapDecoration> decorations = ObfuscationReflectionHelper.getPrivateValue(MapItemSavedData.class, mapData, "decorations");

            ObfuscationReflectionHelper.setPrivateValue(MapItemSavedData.class, mapData, true, "locked");

            if (decorations != null) {
                byte dx = (byte)(int)((pos.getX() - centerX) * 2.0 / (1 << scale));
                byte dz = (byte)(int)((pos.getZ() - centerZ) * 2.0 / (1 << scale));
                decorations.put(
                        "+quest_" + level.getRandom().nextInt(10000),
                        new MapDecoration(MapDecoration.Type.RED_X, dx, dz, (byte) 0, null)
                );
            }
        }
        stack.setHoverName(Component.translatable("filled_map.buried_treasure"));
        return stack;
    }
}

package com.danilon4ig.wt_quests.api;

import com.danilon4ig.wt_quests.Config;
import com.danilon4ig.wt_quests.block.entity.TreasureBoxBlockEntity;
import com.danilon4ig.wt_quests.command.QuestCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.UUID;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuestApi {
    private static final Random RANDOM = new Random();

    private static boolean ftbInited;
    private static boolean ftbLoaded;
    private static Method ftbGetChunk;
    private static Method ftbGetTeamData;
    private static Object ftbManager;
    private static Constructor<?> ftbChunkDimPosCtor;

    @SuppressWarnings("unused")
    public static ItemStack spawnChestQuest(ServerLevel level) {
        return spawnChestQuest(level, level.getSharedSpawnPos(), ForgeRegistries.BLOCKS.getValue(new ResourceLocation("chest")), null);
    }

    @SuppressWarnings("unused")
    public static ItemStack spawnChestQuest(ServerLevel level, BlockPos referencePos) {
        return spawnChestQuest(level, referencePos, ForgeRegistries.BLOCKS.getValue(new ResourceLocation("chest")), null);
    }

    public static ItemStack spawnChestQuest(ServerLevel level, BlockPos referencePos, Block chestBlock, @Nullable CompoundTag contents) {
        return spawnChestQuest(level, referencePos, chestBlock, contents, null);
    }

    public static ItemStack spawnChestQuest(ServerLevel level, BlockPos referencePos, Block chestBlock, @Nullable CompoundTag contents, @Nullable UUID owner) {
        BlockPos chestPos = findBiomePosition(level, referencePos);
        if (chestPos != null) {
            chestPos = randomizeInBiome(level, chestPos);
        } else {
            chestPos = findFallbackPosition(level, referencePos);
        }

        chestPos = findLand(level, chestPos);
        placeChest(level, chestPos, chestBlock, contents, owner);
        return createTreasureMap(level, chestPos);
    }

    private static boolean isBeachBiome(ServerLevel level, BlockPos pos) {
        Holder<Biome> holder = level.getBiome(pos);
        for (String biomeId : Config.biomes) {
            ResourceLocation id = ResourceLocation.tryParse(biomeId);
            if (id != null && holder.is(ResourceKey.create(Registries.BIOME, id))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static BlockPos findBiomePosition(ServerLevel level, BlockPos refPos) {
        int radius = Config.searchRadius;
        for (int r = 0; r <= radius; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    BlockPos candidate = refPos.offset(dx, 0, dz);
                    if (isBeachBiome(level, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos randomizeInBiome(ServerLevel level, BlockPos pos) {
        List<BlockPos> sandPositions = new ArrayList<>();
        List<BlockPos> landPositions = new ArrayList<>();

        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                BlockPos candidate = pos.offset(dx, 0, dz);
                if (!isBeachBiome(level, candidate)) continue;
                if (isClaimedByFTB(level, candidate)) continue;

                BlockPos surface = getSurfacePos(level, candidate);
                if (isWater(level, surface)) continue;
                if (!isValidSurface(level, surface)) continue;

                if (isSand(level, surface) || isSand(level, surface.below())) {
                    sandPositions.add(surface);
                } else {
                    landPositions.add(surface);
                }
            }
        }

        if (!sandPositions.isEmpty()) {
            return sandPositions.get(RANDOM.nextInt(sandPositions.size()));
        }
        if (!landPositions.isEmpty()) {
            return landPositions.get(RANDOM.nextInt(landPositions.size()));
        }
        return pos;
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

    private static boolean isValidSurface(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.LEAVES)) return false;
        if (state.is(BlockTags.LOGS)) return false;
        return state.isSolid();
    }

    private static boolean isWater(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER);
    }

    private static boolean isSand(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND);
    }

    private static BlockPos findLand(ServerLevel level, BlockPos pos) {
        BlockPos surface = getSurfacePos(level, pos);
        if (!isWater(level, surface)) {
            return surface;
        }

        for (int r = 1; r <= 10; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;

                    BlockPos candidate = pos.offset(dx, 0, dz);
                    BlockPos land = getSurfacePos(level, candidate);
                    if (!isWater(level, land)) {
                        return land;
                    }
                }
            }
        }
        return surface;
    }

    private static boolean isClaimedByFTB(ServerLevel level, BlockPos pos) {
        if (!ftbInited) {
            ftbLoaded = ModList.get().isLoaded("ftbchunks");
            if (ftbLoaded) {
                try {
                    Class<?> apiClass = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
                    Object api = apiClass.getField("instance").get(null);
                    ftbManager = api.getClass().getMethod("getManager").invoke(api);

                    Class<?> cdpClass = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");
                    ftbChunkDimPosCtor = cdpClass.getConstructor(ChunkPos.class, ResourceKey.class);

                    ftbGetChunk = ftbManager.getClass().getMethod("getChunk", cdpClass);
                } catch (Exception e) {
                    ftbLoaded = false;
                }
            }
            ftbInited = true;
        }

        if (!ftbLoaded || ftbManager == null) return false;

        try {
            ChunkPos chunkPos = new ChunkPos(pos);
            Object dimKey = level.dimension();
            Object cdp = ftbChunkDimPosCtor.newInstance(chunkPos, dimKey);

            Object claimedChunk = ftbGetChunk.invoke(ftbManager, cdp);
            if (claimedChunk != null) {
                if (ftbGetTeamData == null) {
                    ftbGetTeamData = claimedChunk.getClass().getMethod("getTeamData");
                }
                Object teamData = ftbGetTeamData.invoke(claimedChunk);
                return teamData != null;
            }
        } catch (Exception ignored) {
            ftbLoaded = false;
        }
        return false;
    }

    @SuppressWarnings("unused")
    private static void placeChest(ServerLevel level, BlockPos pos, Block block, @Nullable CompoundTag contents) {
        placeChest(level, pos, block, contents, null);
    }

    private static void placeChest(ServerLevel level, BlockPos pos, Block block, @Nullable CompoundTag contents, @Nullable UUID owner) {
        level.setBlock(pos, block.defaultBlockState(), 3);

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TreasureBoxBlockEntity box) {
            if (owner != null) {
                box.setOwner(owner);
                QuestCommand.markChestActive(owner, pos);
            }
            if (contents != null) {
                ListTag items = contents.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < items.size(); i++) {
                    CompoundTag itemTag = items.getCompound(i);
                    int slot = itemTag.getByte("Slot") & 255;
                    ItemStack stack = ItemStack.of(itemTag);
                    if (!stack.isEmpty() && slot < 9) {
                        box.getItemStackHandler().setStackInSlot(slot, stack);
                    }
                }
            }
        } else if (contents != null && be instanceof RandomizableContainerBlockEntity container) {
            ListTag items = contents.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag itemTag = items.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                ItemStack stack = ItemStack.of(itemTag);
                if (!stack.isEmpty() && slot < container.getContainerSize()) {
                    container.setItem(slot, stack);
                }
            }
        }
    }

    private static ItemStack createTreasureMap(ServerLevel level, BlockPos pos) {
        ItemStack stack = MapItem.create(level, pos.getX(), pos.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, stack);
        MapItemSavedData mapData = MapItem.getSavedData(stack, level);
        if (mapData != null) {
            try {
                for (Field f : MapItemSavedData.class.getDeclaredFields()) {
                    f.setAccessible(true);
                    if (f.getType() == boolean.class && !f.getBoolean(mapData)) {
                        f.setBoolean(mapData, true);
                        break;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to lock treasure map", e);
            }
            MapItemSavedData.addTargetDecoration(stack, pos, "+quest_" + level.getRandom().nextInt(10000), MapDecoration.Type.RED_X);
        }
        stack.setHoverName(Component.translatable("item.wt_quests.treasure_map"));
        return stack;
    }
}

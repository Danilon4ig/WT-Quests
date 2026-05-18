package com.danilon4ig.wt_quests.block.entity;

import com.danilon4ig.wt_quests.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.danilon4ig.wt_quests.Wt_quests.MODID;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    public static final RegistryObject<BlockEntityType<TreasureBoxBlockEntity>> TREASURE_BOX_BE =
            BLOCK_ENTITIES.register("starter_treasure_be", () -> BlockEntityType.Builder.of(TreasureBoxBlockEntity::new,
                    ModBlocks.TREASURE_BOX_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

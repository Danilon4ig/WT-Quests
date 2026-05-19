package com.danilon4ig.wt_quests.block;

import com.danilon4ig.wt_quests.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

import static com.danilon4ig.wt_quests.Wt_quests.MODID;

public class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> object = BLOCKS.register(name, block);
        registerBlockItem(name, object);
        return object;
    }

    public static final RegistryObject<Block> TREASURE_BOX_BLOCK = registerBlock("starter_treasure",
            () -> new TreasureBoxBlock(BlockBehaviour.Properties.of().noOcclusion().strength(-1.0F, 3600000.0F)));

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

package com.danilon4ig.wt_quests.command;

import com.danilon4ig.wt_quests.Wt_quests;
import com.danilon4ig.wt_quests.api.QuestApi;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = Wt_quests.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QuestCommand {

    private static final DynamicCommandExceptionType INVALID_CHEST = new DynamicCommandExceptionType(
            id -> Component.literal("Invalid chest type: " + id));

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wt_quests")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("give")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("chest_type", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("chest");
                                                    builder.suggest("barrel");
                                                    builder.suggest("trapped_chest");
                                                    builder.suggest("shulker_box");
                                                    return builder.buildFuture();
                                                })
                                                .then(Commands.argument("contents", CompoundTagArgument.compoundTag())
                                                        .executes(ctx -> executeGive(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "targets"),
                                                                ResourceLocationArgument.getId(ctx, "chest_type"),
                                                                CompoundTagArgument.getCompoundTag(ctx, "contents")
                                                        ))
                                                )
                                                .executes(ctx -> executeGive(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                        ResourceLocationArgument.getId(ctx, "chest_type"),
                                                        null
                                                ))
                                        )
                                )
                        )
        );
    }

    private static int executeGive(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation chestId, CompoundTag contents) throws CommandSyntaxException {
        if (!ForgeRegistries.BLOCKS.containsKey(chestId)) {
            throw INVALID_CHEST.create(chestId);
        }

        Block block = ForgeRegistries.BLOCKS.getValue(chestId);
        ServerLevel level = source.getLevel();

        for (ServerPlayer target : targets) {
            ItemStack map = QuestApi.spawnChestQuest(level, target.blockPosition(), block, contents);
            target.getInventory().add(map);
        }

        source.sendSuccess(() -> Component.literal("Chest spawned for " + targets.size() + " player(s). Treasure map added."), true);
        return targets.size();
    }
}

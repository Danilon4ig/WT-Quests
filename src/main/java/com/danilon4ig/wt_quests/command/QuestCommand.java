package com.danilon4ig.wt_quests.command;

import com.danilon4ig.wt_quests.Wt_quests;
import com.danilon4ig.wt_quests.api.QuestApi;
import com.danilon4ig.wt_quests.block.entity.TreasureBoxBlockEntity;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Wt_quests.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QuestCommand {

    private static final Map<UUID, BlockPos> ACTIVE_CHESTS = new HashMap<>();

    public static void markChestActive(UUID playerUuid, BlockPos pos) {
        ACTIVE_CHESTS.put(playerUuid, pos);
    }

    public static void markChestLooted(UUID playerUuid) {
        ACTIVE_CHESTS.remove(playerUuid);
    }

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
                                                    builder.suggest("wt_quests:starter_treasure");
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
                        .then(Commands.literal("clear")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> executeClear(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target")
                                        ))
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
            if (hasActiveChest(target)) {
                source.sendFailure(Component.literal(target.getName().getString() + " already has an active treasure chest!"));
                continue;
            }
            ItemStack map = QuestApi.spawnChestQuest(level, target.blockPosition(), block, contents, target.getUUID());
            target.getInventory().add(map);
        }

        source.sendSuccess(() -> Component.literal("Chest spawned for " + targets.size() + " player(s). Treasure map added."), true);
        return targets.size();
    }

    private static boolean hasActiveChest(ServerPlayer player) {
        return ACTIVE_CHESTS.containsKey(player.getUUID());
    }

    private static int executeClear(CommandSourceStack source, ServerPlayer target) {
        UUID uuid = target.getUUID();
        BlockPos pos = ACTIVE_CHESTS.remove(uuid);

        if (pos != null) {
            for (ServerLevel serverLevel : source.getServer().getAllLevels()) {
                if (serverLevel.getBlockEntity(pos) instanceof TreasureBoxBlockEntity box && uuid.equals(box.getOwner())) {
                    serverLevel.removeBlock(pos, false);
                    source.sendSuccess(() -> Component.literal("Cleared chest for " + target.getName().getString()), true);
                    return 1;
                }
            }
        }
        source.sendSuccess(() -> Component.literal("No active chest found for " + target.getName().getString()), true);
        return 1;
    }
}

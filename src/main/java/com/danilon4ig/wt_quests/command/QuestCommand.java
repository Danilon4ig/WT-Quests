package com.danilon4ig.wt_quests.command;

import com.danilon4ig.wt_quests.Wt_quests;
import com.danilon4ig.wt_quests.api.QuestApi;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Wt_quests.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QuestCommand {
    private final static boolean DEBUG = false;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (DEBUG) {
            event.getDispatcher().register(
                    Commands.literal("wt_quests")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.literal("test")
                                    .executes(ctx -> executeTest(ctx.getSource()))
                            )
            );
        }
    }

    private static int executeTest(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack map = QuestApi.spawnChestQuest((ServerLevel) player.level(), player.blockPosition());
        player.getInventory().add(map);
        source.sendSuccess(() -> Component.literal("Chest spawned! Treasure map added to inventory."), true);
        return 1;
    }
}

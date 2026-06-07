package com.example.command;

import com.example.api.Gravity;
import com.example.api.GravityChanger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

public class GravityCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess,
            Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("gravity")
                .then(Commands.argument("direction", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (Gravity g : Gravity.values()) {
                                builder.suggest(g.name().toLowerCase());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String dir = StringArgumentType.getString(context, "direction").toUpperCase();
                            try {
                                Gravity gravity = Gravity.valueOf(dir);
                                if (context.getSource().getEntity() instanceof GravityChanger changer) {
                                    changer.setGravity(gravity);
                                    context.getSource()
                                            .sendSuccess(() -> Component.literal("Gravity set to " + gravity.name()), true);
                                } else {
                                    context.getSource()
                                            .sendFailure(Component.literal("Entity does not support gravity changing"));
                                }
                            } catch (IllegalArgumentException e) {
                                context.getSource().sendFailure(Component.literal("Invalid direction. Valid: " + Arrays
                                        .stream(Gravity.values()).map(Enum::name).collect(Collectors.joining(", "))));
                            }
                            return 1;
                        })));
    }
}

package com.guflimc.cloud.minestom;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.internal.CommandRegistrationHandler;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class MinestomCommandRegistrationHandler<C> implements CommandRegistrationHandler {

    private MinestomCommandManager<C> commandManager;

    void initialize(final @NotNull MinestomCommandManager<C> commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public boolean registerCommand(@NotNull Command<?> command) {
        MinestomCloudCommand.setup(commandManager, (Command<C>) command);
        return true;
    }
}

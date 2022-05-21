package com.guflimc.cloud.minestom;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.StaticArgument;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.arguments.standard.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.*;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentUUID;
import net.minestom.server.command.builder.arguments.number.ArgumentDouble;
import net.minestom.server.command.builder.arguments.number.ArgumentFloat;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.arguments.number.ArgumentNumber;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MinestomCloudCommand<C> extends Command {

//    private final CommandExecutor emptyExecutor = (sender, args) -> {
//    };

    private final BiConsumer<CommandSender, String> executor;

    public MinestomCloudCommand(BiConsumer<CommandSender, String> executor, String name, String... aliases) {
        super(name, aliases);
        this.executor = executor;
    }

    @Override
    public void globalListener(@NotNull CommandSender sender, @NotNull CommandContext context, @NotNull String command) {
        if ( executor == null ) return;
        String input = command.startsWith("/") ? command.substring(1) : command;
        executor.accept(sender, input);
    }

    //

    private static <C> BiConsumer<CommandSender, String> executor(MinestomCommandManager<C> manager) {
        return (commandSender, s) -> manager.executeCommand((C) commandSender, s);
    }

    public static <C> void setup(MinestomCommandManager<C> manager, cloud.commandframework.Command<C> cloudCommand) {
        // setup root command
        MinestomCloudCommand<C> root = setup(null, null, (StaticArgument<C>) cloudCommand.getArguments().get(0));
        MinestomCloudCommand<C> parent = root;

        // setup sub commands
        int index = 1;
        for (int i = index; i < cloudCommand.getArguments().size(); i++) {
            CommandArgument<C, ?> argument = cloudCommand.getArguments().get(i);
            if (!(argument instanceof StaticArgument arg)) {
                break;
            }

            MinestomCloudCommand<C> cmd = setup(manager, parent, (StaticArgument<C>) arg);
            parent.addSubcommand(cmd);
            parent = cmd;

            index++;
        }

        // hidden cloud command
        if ( cloudCommand.isHidden() ) {
            root.setCondition((sender, commandString) -> commandString != null);
        }

        // setup dynamic arguments
        List<Argument<?>> arguments = new ArrayList<>();
        for (int i = index; i < cloudCommand.getArguments().size(); i++) {
            CommandArgument<C, ?> argument = cloudCommand.getArguments().get(i);
            Argument<?> minestomArgument = convertArgument(argument);
            arguments.add(minestomArgument);

            minestomArgument.setSuggestionCallback((sender, context, suggestion) -> {
                manager.suggest(manager.mapCommandSender(sender), context.getInput())
                        .forEach(str -> suggestion.addEntry(new SuggestionEntry(str)));
            });
        }

        // create minestom syntax for given arguments
        String prefix = cloudCommand.getArguments().subList(0, index).stream().map(CommandArgument::getName)
                .collect(Collectors.joining(" "));

        parent.addSyntax((sender, context) -> {
            manager.executeCommand((C) sender, prefix + context.getInput());
        }, arguments.toArray(Argument[]::new));

        // register root command if not already reigstered
        if ( MinecraftServer.getCommandManager().getCommand(root.getName()) == null ) {
            MinecraftServer.getCommandManager().register(root);
        }
    }

    public static <C> MinestomCloudCommand<C> setup(MinestomCommandManager<C> manager, MinestomCloudCommand<C> parent, StaticArgument<C> argument) {
        String label = argument.getName();
        String[] aliases = argument.getAlternativeAliases().toArray(String[]::new);

        MinestomCloudCommand<C> cmd;
        if ( parent == null ) {
            // find existing root command
            cmd = (MinestomCloudCommand<C>) MinecraftServer.getCommandManager().getCommand(label);
        } else {
            // find existing sub command
            cmd = (MinestomCloudCommand<C>) parent.getSubcommands().stream()
                    .filter(c -> c.getName().equals(label))
                    .findFirst().orElse(null);
        }

        // no existing command exists
        if ( cmd == null ) {
            if ( manager != null ) {
                cmd = new MinestomCloudCommand<>(executor(manager), label, aliases);
            } else {
                cmd = new MinestomCloudCommand<>(null, label, aliases);
            }
        }

        return cmd;
    }

    private static <C> Argument<?> convertArgument(CommandArgument<C, ?> arg) {
        ArgumentParser<C, ?> parser = arg.getParser();
        if (parser instanceof StringArgument.StringParser sp && sp.getStringMode() == StringArgument.StringMode.SINGLE) {
            ArgumentWord result = new ArgumentWord((arg.getName()));
            result.setDefaultValue(arg.getDefaultValue());
            return result;
        }

        if (parser instanceof StringArgument.StringParser sp && sp.getStringMode() == StringArgument.StringMode.GREEDY) {
            ArgumentStringArray result = new ArgumentStringArray((arg.getName()));
            result.setDefaultValue(arg.getDefaultValue().split(Pattern.quote(" ")));
            return result;
        }

        if (parser instanceof StringArgument.StringParser sp && sp.getStringMode() == StringArgument.StringMode.QUOTED) {
            ArgumentString result = new ArgumentString((arg.getName()));
            result.setDefaultValue(arg.getDefaultValue());
            return result;
        }

        if (arg instanceof FloatArgument fa) {
            ArgumentNumber<Float> result = new ArgumentFloat(arg.getName())
                    .min(fa.getMin()).max(fa.getMax());
            if (!fa.getDefaultValue().equals("")) {
                result.setDefaultValue(Float.parseFloat(fa.getDefaultValue()));
            }
            return result;
        }

        if (arg instanceof DoubleArgument da) {
            ArgumentNumber<Double> result = new ArgumentDouble(arg.getName())
                    .min(da.getMin()).max(da.getMax());
            if (!da.getDefaultValue().equals("")) {
                result.setDefaultValue(Double.parseDouble(da.getDefaultValue()));
            }
            return result;
        }

        if (arg instanceof IntegerArgument ia) {
            ArgumentNumber<Integer> result = new ArgumentInteger(arg.getName())
                    .min(ia.getMax()).max(ia.getMax());
            if (!ia.getDefaultValue().equals("")) {
                result.setDefaultValue(Integer.parseInt(ia.getDefaultValue()));
            }
            return result;
        }

        if (arg instanceof UUIDArgument ua) {
            ArgumentUUID result = new ArgumentUUID(arg.getName());
            if (!ua.getDefaultValue().equals("")) {
                result.setDefaultValue(UUID.fromString(ua.getDefaultValue()));
            }
            return result;
        }

        if (arg instanceof BooleanArgument ba) {
            ArgumentBoolean result = new ArgumentBoolean(arg.getName());
            if (!ba.getDefaultValue().equals("")) {
                result.setDefaultValue(Boolean.parseBoolean(ba.getDefaultValue()));
            }
            return result;
        }

//        if ( arg instanceof EntityTypeArgument eta ) {
//            ArgumentEntityType result = new ArgumentEntityType(arg.getName());
//            if (!eta.getDefaultValue().equals("")) {
//                result.setDefaultValue(EntityType.fromNamespaceId(eta.getDefaultValue()));
//            }
//            return result;
//        }

        return new ArgumentWord(arg.getName())
                .setDefaultValue(arg.getDefaultValue());
    }

}

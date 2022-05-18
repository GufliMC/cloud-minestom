package io.github.openminigameserver.cloudminestom;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.StaticArgument;
import cloud.commandframework.arguments.standard.*;
import net.minestom.server.command.builder.arguments.*;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentUUID;
import net.minestom.server.command.builder.arguments.number.ArgumentDouble;
import net.minestom.server.command.builder.arguments.number.ArgumentFloat;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import org.jetbrains.annotations.NotNull;

final class CloudInteropHelper {

    @NotNull
    protected static String removeSlashPrefix(@NotNull String text) {
        String command = text;
        if (command.startsWith("/"))
            command = command.substring(1);
        return command;
    }

    protected static <C> Argument<?> convertCloudArgumentToMinestom(CommandArgument<C, ?> arg) {
        Argument<?> result;
        if (arg instanceof StaticArgument) {
            result =
                    new ArgumentWord(arg.getName()).from(((StaticArgument<?>) arg).getAliases().toArray(String[]::new));
        } else {
            var parser = arg.getParser();
            if (arg instanceof StringArgument && ((StringArgument<?>) arg).getStringMode() == StringArgument.StringMode.GREEDY) {
                result = new ArgumentStringArray((arg.getName()));
            } else if (parser instanceof StringArgument.StringParser && ((StringArgument.StringParser<?>) parser).getStringMode() == StringArgument.StringMode.GREEDY) {
                result = new ArgumentStringArray((arg.getName()));
            }
            else if ( arg instanceof FloatArgument ) {
                result = new ArgumentFloat(arg.getName());
            }
            else if ( arg instanceof DoubleArgument ) {
                result = new ArgumentDouble(arg.getName());
            }
            else if ( arg instanceof IntegerArgument ) {
                result = new ArgumentInteger(arg.getName());
            }
            else if ( arg instanceof UUIDArgument ) {
                result = new ArgumentUUID(arg.getName());
            }
            else if ( arg instanceof BooleanArgument ) {
                result = new ArgumentBoolean(arg.getName());
            }
            else {
                result = new ArgumentString(arg.getName());
            }
        }
        return result;
    }
}

package io.github.openminigameserver.cloudminestom;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.CommandSyntax;
import net.minestom.server.command.builder.arguments.Argument;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletionException;

public class MinestomCloudCommand<C> extends Command {

    private final MinestomCommandManager<C> manager;
    private final CommandExecutor emptyExecutor = (sender, args) -> {};

    public MinestomCloudCommand(cloud.commandframework.Command<C> cloudCommand, MinestomCommandManager<C> manager,
                                String name, String... aliases) {
        super(name, aliases);
        this.manager = manager;

        if (cloudCommand.isHidden()) {
            setCondition((sender, commandString) -> commandString != null);
        }

        registerCommandArguments(cloudCommand);
    }

    @NotNull
    private static String[] getArgumentNamesFromArguments(Argument<?>[] arguments) {
        return Arrays.stream(arguments).map(Argument::getId).toArray(String[]::new);
    }

    public void registerCommandArguments(cloud.commandframework.Command<?> cloudCommand) {
        setDefaultExecutor(emptyExecutor);

        Argument<?>[] arguments = cloudCommand.getArguments().stream()
                .skip(1)
                .map(CloudInteropHelper::convertCloudArgumentToMinestom)
                .toArray(Argument[]::new);

        boolean containsSyntax = getSyntaxes().stream().anyMatch(syntax ->
                Arrays.equals(getArgumentNamesFromArguments(arguments),
                        getArgumentNamesFromArguments(syntax.getArguments())));

        Collection<CommandSyntax> syntaxes = getSyntaxes();

        if ( !containsSyntax && arguments.length != 0 ) {
            addSyntax(emptyExecutor, arguments);

//            var toMove = syntaxes.stream()
//                    .filter(it -> Arrays.stream(it.getArguments()).allMatch(arg -> arg instanceof ArgumentString)
//            ).collect(Collectors.toList());
//
//            syntaxes.removeAll(toMove);
//            syntaxes.addAll(0, toMove);

        }
    }

    @Override
    public void globalListener(@NotNull CommandSender commandSender, @NotNull CommandContext context,
                               @NotNull String command) {
        var input = CloudInteropHelper.removeSlashPrefix(command);
        final C sender = this.manager.mapCommandSender(commandSender);
        this.manager.executeCommand(sender, input)
                .whenComplete((commandResult, throwable) -> {
                    if (throwable == null) {
                        return;
                    }

                    if (throwable instanceof CompletionException) {
                        throwable = throwable.getCause();
                    }

                    Exception ex = (Exception) throwable;
                    this.manager.handleException(sender, Exception.class, ex,
                            (c, e) -> commandSender.sendMessage(e.getMessage()));
                });

    }

//    @Nullable
//    @Override
//    public String[] onDynamicWrite(@NotNull CommandSender sender, @NotNull String text) {
//        return manager.suggest(manager.mapCommandSender(sender), removeSlashPrefix(text)).toArray(String[]::new);
//    }

}

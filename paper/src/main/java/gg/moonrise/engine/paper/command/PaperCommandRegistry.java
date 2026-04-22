package gg.moonrise.engine.paper.command;

import gg.moonrise.engine.command.CloudArgument;
import gg.moonrise.engine.paper.PaperPlugin;
import gg.moonrise.moss.spring.Enableable;
import gg.moonrise.moss.spring.SpringComponent;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.ParserRegistry;

@SpringComponent
@RequiredArgsConstructor
@Slf4j
public class PaperCommandRegistry implements Enableable {

    private final PaperPlugin plugin;

    @Override
    public void onEnable() {
        log.info("Registering commands");
        PaperCommandManager<CommandSourceStack> commands = PaperCommandManager.builder()
                .executionCoordinator(ExecutionCoordinator.asyncCoordinator())
                .buildOnEnable(plugin);

        ParserRegistry<CommandSourceStack> parser = commands.parserRegistry();
        plugin.fetchBeans(CloudArgument.class, argument -> {
            String name = argument.name();
            if (name == null) {
                parser.registerParser(ParserDescriptor.of(argument, argument.getType()));
            } else {
                parser.registerNamedParser(name, ParserDescriptor.of(argument, argument.getType()));
            }
            log.info("Parsed argument: {}", argument.getClass().getSimpleName());
        }, (argument, e) -> log.error("Could not register argument parser: {}", argument.getClass().getSimpleName(), e));

        AnnotationParser<CommandSourceStack> annotationParser = new AnnotationParser<>(commands, CommandSourceStack.class);

        plugin.invokeBeans(PaperCommand.class, command -> {
            command.onRegister(commands);
            annotationParser.parse(command);
            log.info("Registered command: {}", command.getClass().getSimpleName());
        }, (command, e) -> log.error("Could not register command: {}", command.getClass().getSimpleName(), e));
    }
}

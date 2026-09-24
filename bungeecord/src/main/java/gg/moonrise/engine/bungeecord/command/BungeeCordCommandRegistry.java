package gg.moonrise.engine.bungeecord.command;

import gg.moonrise.engine.bungeecord.BungeePlugin;
import gg.moonrise.engine.command.CloudArgument;
import gg.moonrise.moss.spring.Enableable;
import gg.moonrise.moss.spring.SpringComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.md_5.bungee.api.CommandSender;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.bungee.BungeeCommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.ParserRegistry;

@SpringComponent
@RequiredArgsConstructor
@Slf4j
public class BungeeCordCommandRegistry implements Enableable {

    private final BungeePlugin plugin;

    @Override
    public void onEnable() {
        log.info("Registering commands");
        BungeeCommandManager<CommandSender> commands = new BungeeCommandManager<>(
                plugin,
                ExecutionCoordinator.asyncCoordinator(),
                SenderMapper.identity()
        );

        ParserRegistry<CommandSender> parser = commands.parserRegistry();
        plugin.fetchBeans(CloudArgument.class, argument -> {
            String name = argument.name();
            if (name == null) {
                parser.registerParser(ParserDescriptor.of(argument, argument.getType()));
            } else {
                parser.registerNamedParser(name, ParserDescriptor.of(argument, argument.getType()));
            }
            log.info("Parsed argument: {}", argument.getClass().getSimpleName());
        }, (argument, e) -> log.error("Could not register argument parser: {}", argument.getClass().getSimpleName(), e));

        AnnotationParser<CommandSender> annotationParser = new AnnotationParser<>(commands, CommandSender.class);

        plugin.invokeBeans(BungeeCordCommand.class, command -> {
            if (!command.shouldEnable()) return;

            command.onRegister(commands);
            annotationParser.parse(command);
            log.info("Registered command: {}", command.getClass().getSimpleName());
        }, (command, e) -> log.error("Could not register command: {}", command.getClass().getSimpleName(), e));
    }
}

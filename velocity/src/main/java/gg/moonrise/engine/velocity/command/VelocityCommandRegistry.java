package gg.moonrise.engine.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import gg.moonrise.engine.command.CloudArgument;
import gg.moonrise.engine.velocity.VelocityPlugin;
import gg.moonrise.moss.spring.Enableable;
import gg.moonrise.moss.spring.SpringComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.ParserRegistry;
import org.incendo.cloud.velocity.VelocityCommandManager;

@SpringComponent
@RequiredArgsConstructor
@Slf4j
public class VelocityCommandRegistry implements Enableable {

    private final VelocityPlugin plugin;
    private final ProxyServer server;

    @Override
    public void onEnable() {
        log.info("Registering commands");

        PluginContainer container = server.getPluginManager().ensurePluginContainer(plugin);
        VelocityCommandManager<CommandSource> commands = new VelocityCommandManager<>(
                container,
                server,
                ExecutionCoordinator.asyncCoordinator(),
                SenderMapper.identity()
        );

        ParserRegistry<CommandSource> parserRegistry = commands.parserRegistry();
        plugin.fetchBeans(CloudArgument.class, argument -> {
            String name = argument.name();
            if (name == null) {
                parserRegistry.registerParser(ParserDescriptor.of(argument, argument.getType()));
            } else {
                parserRegistry.registerNamedParser(name, ParserDescriptor.of(argument, argument.getType()));
            }
            log.info("Parsed argument: {}", argument.getClass().getSimpleName());
        }, (argument, exception) -> log.error(
                "Could not register argument parser: {}",
                argument.getClass().getSimpleName(),
                exception
        ));

        AnnotationParser<CommandSource> annotationParser = new AnnotationParser<>(commands, CommandSource.class);
        plugin.fetchBeans(VelocityCommand.class, command -> {
            if (!command.shouldEnable()) {
                return;
            }

            command.onRegister(commands);
            annotationParser.parse(command);
            log.info("Registered command: {}", command.getClass().getSimpleName());
        }, (command, exception) -> log.error(
                "Could not register command: {}",
                command.getClass().getSimpleName(),
                exception
        ));
    }
}

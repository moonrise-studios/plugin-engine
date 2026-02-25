package games.negative.engine.paper.command.argument;

import games.negative.engine.command.CloudArgument;
import games.negative.moss.spring.SpringComponent;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.Locale;

@SpringComponent
public class PlayerArgument implements CloudArgument<CommandSourceStack, Player> {

    @Override
    public Class<Player> getType() {
        return Player.class;
    }

    @Override
    public @NonNull ArgumentParseResult<Player> parse(
            @NonNull CommandContext<CommandSourceStack> context,
            @NonNull CommandInput input
    ) {
        String string = input.readString();

        Player player = Bukkit.getPlayer(string);
        return resultOrThrow(player, () -> new NullPointerException("Player \"" + string + "\" not found"));
    }

    @Override
    public @NonNull SuggestionProvider<CommandSourceStack> suggestionProvider() {
        return SuggestionProvider.blockingStrings((context, input) -> {
            String prefix = input.lastRemainingToken().toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .filter(player -> context.sender().getExecutor() == null || !(context.sender().getSender() instanceof Player senderPlayer) || senderPlayer.canSee(player))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        });
    }

}

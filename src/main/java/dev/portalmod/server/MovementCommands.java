package dev.portalmod.server;

import dev.portalmod.config.ConfigManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class MovementCommands {
    private MovementCommands() { }
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> dispatcher.register(
                Commands.literal("portalmod")
                        .then(Commands.literal("status").executes(context -> {
                            context.getSource().sendSuccess(() -> Component.literal("PortalMC movement: "
                                    + (ConfigManager.server().enabled ? "Portal" : "vanilla")), false);
                            return 1;
                        }))
                        .then(Commands.literal("movement").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.literal("portal").executes(context -> setMode(true, context.getSource())))
                                .then(Commands.literal("vanilla").executes(context -> setMode(false, context.getSource()))))
                        .then(Commands.literal("reload").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(context -> {
                                    try {
                                        ConfigManager.load();
                                        ServerMovement.reloadSessions();
                                        context.getSource().sendSuccess(() -> Component.literal("PortalMC config reloaded."), true);
                                        return 1;
                                    } catch (RuntimeException e) {
                                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                                        return 0;
                                    }
                                }))));
    }

    private static int setMode(boolean enabled, net.minecraft.commands.CommandSourceStack source) {
        boolean previous = ConfigManager.server().enabled;
        ConfigManager.server().enabled = enabled;
        try {
            ConfigManager.save();
        } catch (RuntimeException e) {
            ConfigManager.server().enabled = previous;
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
        ServerMovement.reloadSessions();
        source.sendSuccess(() -> Component.literal("PortalMC movement changed to " + (enabled ? "Portal" : "vanilla") + "."), true);
        return 1;
    }
}

package dev.portalmod;

import net.fabricmc.api.ModInitializer;
import dev.portalmod.config.ConfigManager;
import dev.portalmod.network.MovementPayloads;
import dev.portalmod.server.ServerMovement;
import dev.portalmod.server.MovementCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entrypoint: must never import Minecraft client classes. */
public final class PortalMod implements ModInitializer {
    public static final String MOD_ID = "portalmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PortalItems.initialize();
        ConfigManager.load();
        MovementPayloads.register();
        ServerMovement.initialize();
        MovementCommands.initialize();
        dev.portalmod.portal.PortalServer.initialize();
        LOGGER.info("PortalMC initialized; Portal movement enabled: {}", ConfigManager.server().enabled);
    }
}

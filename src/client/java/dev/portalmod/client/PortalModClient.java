package dev.portalmod.client;

import dev.portalmod.PortalMod;
import net.fabricmc.api.ClientModInitializer;
import dev.portalmod.client.movement.ClientMovement;

/** Client rendering and input will live in this source set in later phases. */
public final class PortalModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        new ClientMovement().initialize();
        PortalMod.LOGGER.info("PortalMC client initialized.");
    }
}

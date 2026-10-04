package dev.portalmod.client;

import dev.portalmod.PortalMod;
import net.fabricmc.api.ClientModInitializer;
import dev.portalmod.client.movement.ClientMovement;
import dev.portalmod.client.assets.LocalPortalAssets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/** Client rendering and input will live in this source set in later phases. */
public final class PortalModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        new ClientMovement().initialize();
        ClientLifecycleEvents.CLIENT_STARTED.register(LocalPortalAssets::load);
        PortalMod.LOGGER.info("PortalMC client initialized.");
    }
}

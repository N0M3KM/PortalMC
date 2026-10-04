package dev.portalmod.client;

import dev.portalmod.PortalMod;
import net.fabricmc.api.ClientModInitializer;
import dev.portalmod.client.movement.ClientMovement;
import dev.portalmod.client.assets.LocalPortalAssets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/** Client-only prediction, local model imports and portal rendering. */
public final class PortalModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        dev.portalmod.client.visual.PortalVisualConfig.load();
        dev.portalmod.client.visual.CharacterAnimation.initialize();
        dev.portalmod.client.visual.PhysicsHud.initialize();
        new ClientMovement().initialize();
        ClientLifecycleEvents.CLIENT_STARTED.register(LocalPortalAssets::load);
        dev.portalmod.client.portal.ClientPortals.initialize();
        PortalMod.LOGGER.info("PortalMC client initialized.");
    }
}

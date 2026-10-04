package dev.portalmod;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Phase 0 only: a harmless item to verify registration on both environments. */
public final class PortalItems {
    public static final ResourceKey<Item> HELLO_WORLD_KEY = ResourceKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(PortalMod.MOD_ID, "hello_world"));
    public static final Item HELLO_WORLD = Registry.register(
            BuiltInRegistries.ITEM,
            HELLO_WORLD_KEY,
            new Item(new Item.Properties().setId(HELLO_WORLD_KEY)));

    private PortalItems() {
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> output.accept(HELLO_WORLD));
    }
}

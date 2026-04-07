package com.azuredoom.hytale.asseteditor.runtime;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;

/**
 * Provides static factory methods for creating and configuring instances of {@link AssetEditorPackBridge}. This class
 * is designed to initialize and manage the integration of asset editor packs within the runtime environment.
 */
public final class AssetEditorRuntime {

    private AssetEditorRuntime() {}

    /**
     * Creates a new instance of {@link AssetEditorPackBridge} using the provided {@link JavaPlugin}. This instance
     * facilitates the integration and management of asset editor packs within the runtime.
     *
     * @param plugin the {@link JavaPlugin} that serves as the primary context for the asset editor pack bridge
     *               creation; must not be null
     * @return a new {@link AssetEditorPackBridge} instance configured for the specified {@link JavaPlugin}
     */
    public static AssetEditorPackBridge create(@Nonnull JavaPlugin plugin) {
        return new DefaultAssetEditorPackBridge(plugin);
    }

    /**
     * Creates a new instance of {@link AssetEditorPackBridge} using the provided {@link JavaPlugin} and runtime
     * configuration.
     * <p>
     * This method enables robust integration of asset editor packs during runtime setup, ensuring that both the plugin
     * context and custom configuration are utilized.
     *
     * @param plugin the {@link JavaPlugin} instance that serves as the primary context for creating the asset editor
     *               pack bridge; must not be null
     * @param config the {@link AssetEditorRuntimeConfig} providing additional runtime configuration; must not be null
     * @return a new {@link AssetEditorPackBridge} instance, configured with the specified plugin and runtime
     *         configuration
     */
    public static AssetEditorPackBridge create(@Nonnull JavaPlugin plugin, @Nonnull AssetEditorRuntimeConfig config) {
        return new DefaultAssetEditorPackBridge(plugin, config);
    }
}

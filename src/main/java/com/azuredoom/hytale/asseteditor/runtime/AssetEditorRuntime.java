package com.azuredoom.hytale.asseteditor.runtime;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

/**
 * Provides static factory methods for creating and configuring instances of {@link AssetEditorPackBridge}. This class
 * is designed to initialize and manage the integration of asset editor packs within the runtime environment.
 */
public final class AssetEditorRuntime {

    private AssetEditorRuntime() {}

    /**
     * Creates an instance of {@link AssetEditorPackBridge} using the provided plugin and default runtime configuration.
     * This static factory method initializes the bridge for managing asset editor pack functionality, integrating it
     * with the given plugin.
     *
     * @param plugin the {@link JavaPlugin} instance that will be used to integrate with the asset editor pack
     *               functionality. Must not be null.
     * @return an initialized {@link AssetEditorPackBridge} instance configured for use with the provided plugin and
     *         default runtime settings.
     */
    public static AssetEditorPackBridge create(JavaPlugin plugin) {
        return new DefaultAssetEditorPackBridge(plugin, AssetEditorRuntimeConfig.defaults());
    }

    /**
     * Creates an instance of {@link AssetEditorPackBridge} using the provided plugin and runtime configuration. This
     * method allows for customized initialization of the bridge based on the given configuration.
     *
     * @param plugin the {@link JavaPlugin} instance that will be integrated with the asset editor pack functionality.
     *               Must not be null.
     * @param config the {@link AssetEditorRuntimeConfig} object containing runtime settings for the asset editor pack.
     *               Must not be null.
     * @return an initialized {@link AssetEditorPackBridge} instance configured for use with the specified plugin and
     *         runtime configuration.
     */
    public static AssetEditorPackBridge create(JavaPlugin plugin, AssetEditorRuntimeConfig config) {
        return new DefaultAssetEditorPackBridge(plugin, config);
    }
}

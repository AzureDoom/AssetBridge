package com.azuredoom.hytale.asseteditor.runtime;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.AssetPackRegisterEvent;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import javax.annotation.Nonnull;

/**
 * Runtime helper that mirrors the jar-backed Asset Editor registration pattern used by AlecsTamework, but packaged as a
 * reusable library.
 * <p>
 * Credit to <a href="https://github.com/Alechilles">Alechilles</a> for the original implementation.
 */
public final class DefaultAssetEditorPackBridge implements AssetEditorPackBridge {

    private static final String ASSET_EDITOR_PLUGIN_CLASS =
        "com.hypixel.hytale.builtin.asseteditor.AssetEditorPlugin";

    private final JavaPlugin plugin;

    private final AssetEditorRuntimeConfig config;

    /**
     * Constructs a new instance of {@code DefaultAssetEditorPackBridge}. This class serves as a bridge for managing
     * asset packs in the Asset Editor plugin, using runtime configurations for specific functionality.
     *
     * @param plugin the {@code JavaPlugin} instance associated with this bridge. Must not be {@code null}.
     */
    public DefaultAssetEditorPackBridge(@Nonnull JavaPlugin plugin) {
        this(plugin, AssetEditorRuntimeConfig.defaults());
    }

    /**
     * Constructs a new instance of {@code DefaultAssetEditorPackBridge}. This class serves as a bridge for managing
     * asset packs in the Asset Editor plugin, using runtime configurations for specific functionality.
     *
     * @param plugin the {@code JavaPlugin} instance associated with this bridge. Must not be {@code null}.
     * @param config the {@code AssetEditorRuntimeConfig} containing the runtime configuration for asset editor
     *               operations. Must not be {@code null}.
     */
    public DefaultAssetEditorPackBridge(
        @Nonnull JavaPlugin plugin,
        @Nonnull AssetEditorRuntimeConfig config
    ) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Registers a hook to handle early asset pack ordering during the asset loading process. This method checks if the
     * runtime configuration and the early asset-pack ordering feature are both enabled before proceeding with the
     * registration. If conditions are met, it registers an event listener for the {@code LoadAssetEvent} with a
     * priority specified in the configuration.
     */
    @Override
    public void registerEarlyAssetPackOrderingHook() {
        if (!config.enabled()) {
            debug("Asset editor runtime disabled; skipping early ordering hook registration.");
            return;
        }

        if (!config.enableEarlyAssetPackOrdering()) {
            debug("Early asset-pack ordering hook disabled by configuration.");
            return;
        }

        plugin.getEventRegistry()
            .register(
                config.earlyAssetPackOrderPriority(),
                LoadAssetEvent.class,
                this::onEarlyAssetLoad
            );

        debug("Registered early asset-pack ordering hook.");
    }

    /**
     * Ensures that the asset pack associated with the current configuration is visible to the Asset Editor. This
     * involves checking if the asset synchronization is enabled in the configuration, identifying the asset pack, and
     * registering it with the Asset Editor if it is not already visible.
     */
    @Override
    public void ensureAssetEditorPackVisible() {
        if (!config.enabled()) {
            debug("Asset editor runtime disabled; skipping AssetEditor synchronization.");
            return;
        }

        var assetModule = AssetModule.get();
        if (assetModule == null) {
            warn("Asset editor sync skipped because AssetModule is unavailable.");
            return;
        }

        var packId = resolvePackId();
        var pack = assetModule.getAssetPack(packId);
        if (pack == null) {
            warn("Asset editor sync skipped because pack '" + packId + "' was not found.");
            return;
        }

        try {
            var assetEditorPluginClass = Class.forName(ASSET_EDITOR_PLUGIN_CLASS);
            var getMethod = assetEditorPluginClass.getMethod("get");
            var assetEditorPlugin = getMethod.invoke(null);
            if (assetEditorPlugin == null) {
                debug("AssetEditor plugin instance unavailable.");
                return;
            }

            var getDataSourceForPack =
                assetEditorPluginClass.getMethod("getDataSourceForPack", String.class);

            if (getDataSourceForPack.invoke(assetEditorPlugin, packId) != null) {
                debug("AssetEditor already has a data source for pack '" + packId + "'.");
                return;
            }

            var onRegisterAssetPack =
                assetEditorPluginClass.getDeclaredMethod(
                    "onRegisterAssetPack",
                    AssetPackRegisterEvent.class
                );
            onRegisterAssetPack.setAccessible(true);
            onRegisterAssetPack.invoke(assetEditorPlugin, new AssetPackRegisterEvent(pack));

            if (getDataSourceForPack.invoke(assetEditorPlugin, packId) != null) {
                info("Registered read-only AssetEditor data source for pack '" + packId + "'.");
                return;
            }

            warn("AssetEditor did not expose a data source for pack '" + packId + "' after registration.");
        } catch (ClassNotFoundException ex) {
            debug("AssetEditor plugin class not found; skipping editor synchronization.");
        } catch (Exception ex) {
            plugin.getLogger()
                .at(Level.WARNING)
                .withCause(ex)
                .log("Asset editor sync failed while registering the embedded pack with AssetEditor.");
        }
    }

    /**
     * Handles early asset pack setup during a {@link LoadAssetEvent}.
     * <p>
     * This method ensures that the correct asset pack is present and properly
     * positioned before the asset system finishes initialization.
     *
     * <p>Specifically, it will:
     * <ul>
     *   <li>Resolve the target asset pack ID and its source location.</li>
     *   <li>Register the asset pack if it has not already been registered.</li>
     *   <li>Replace an existing pack only if it represents a legacy standalone
     *       assets archive (e.g., a generated or outdated zip).</li>
     *   <li>Preserve existing non-legacy packs, even if their location differs.</li>
     *   <li>Adjust pack ordering relative to {@code baseAssetPackId}, when applicable.</li>
     * </ul>
     *
     * <p>This logic is intentionally conservative to avoid unregistering active
     * runtime or development asset packs, which may still be in use by other systems.
     *
     * @param event the {@link LoadAssetEvent} fired during asset system initialization
     */
    private void onEarlyAssetLoad(LoadAssetEvent event) {
        var assetModule = AssetModule.get();
        if (assetModule == null) {
            warn("Asset pack ordering skipped because AssetModule is unavailable during LoadAssetEvent.");
            return;
        }

        var packId = resolvePackId();
        var pluginPackPath = normalizePath(plugin.getFile());

        removeLegacyStandaloneAssetPack(assetModule, packId, pluginPackPath);

        var existingPack = assetModule.getAssetPack(packId);
        if (existingPack != null) {
            var existingPackPath = normalizePath(existingPack.getPackLocation());

            if (!samePath(existingPackPath, pluginPackPath)) {
                if (isLegacyAssetsZip(existingPackPath)) {
                    info(
                            "Replacing legacy pre-registered pack '" + packId + "' from "
                                    + existingPackPath + " with " + pluginPackPath + "."
                    );
                    assetModule.unregisterPack(packId);
                    tryDeleteLegacyAssetsZip(existingPackPath, pluginPackPath);
                } else {
                    debug(
                            "Keeping existing pack '" + packId + "' at " + existingPackPath
                                    + " because it is not a legacy assets zip."
                    );
                }
            }
        }

        if (assetModule.getAssetPack(packId) == null) {
            try {
                assetModule.registerPack(packId, plugin.getFile(), plugin.getManifest(), true);
            } catch (RuntimeException ex) {
                plugin.getLogger()
                    .at(Level.WARNING)
                    .withCause(ex)
                    .log("Failed to register missing embedded pack '" + packId + "'.");
            }
        }

        var packs = assetModule.getAssetPacks();
        var currentIndex = indexOfPack(packs, packId);
        if (currentIndex < 0) {
            warn("Pack '" + packId + "' was not found after registration attempt.");
            return;
        }

        var targetIndex = desiredPackIndex(packs);
        if (currentIndex == targetIndex) {
            debug("Pack '" + packId + "' is already ordered at index " + currentIndex + ".");
            return;
        }

        var pack = packs.remove(currentIndex);
        if (currentIndex < targetIndex) {
            targetIndex--;
        }
        packs.add(targetIndex, pack);

        info("Moved pack '" + packId + "' from index " + currentIndex + " to index " + targetIndex + ".");
    }

    /**
     * Removes a legacy standalone asset pack from the specified asset module if it exists. This involves unregistering
     * the legacy pack and attempting to delete its associated assets ZIP file from the file system.
     *
     * @param assetModule    the {@code AssetModule} instance from which the legacy asset pack should be removed. Must
     *                       not be {@code null}.
     * @param packId         the identifier of the pack whose legacy counterpart should be removed. Must not be
     *                       {@code null}.
     * @param pluginPackPath the {@code Path} to the plugin's asset pack, used to verify the location of the legacy
     *                       pack's assets. Must not be {@code null}.
     */
    private void removeLegacyStandaloneAssetPack(AssetModule assetModule, String packId, Path pluginPackPath) {
        var legacyPackId = packId + " (Assets)";
        var legacyPack = assetModule.getAssetPack(legacyPackId);
        if (legacyPack == null) {
            return;
        }

        var legacyPackPath = normalizePath(legacyPack.getPackLocation());
        info("Removing legacy standalone pack '" + legacyPackId + "' from " + legacyPackPath + ".");
        assetModule.unregisterPack(legacyPackId);
        tryDeleteLegacyAssetsZip(legacyPackPath, pluginPackPath);
    }

    /**
     * Attempts to delete a legacy assets ZIP file if it exists and meets specific conditions. The method verifies the
     * provided path as a legacy assets ZIP file, checks if it resides in the same directory as the plugin pack path,
     * and deletes it if all conditions are satisfied.
     *
     * @param existingPackPath the {@code Path} of the existing legacy assets ZIP file to be deleted. Can be
     *                         {@code null}. If not null, it must represent a legacy assets ZIP file.
     * @param pluginPackPath   the {@code Path} of the plugin pack, used to verify the directory of the legacy assets
     *                         ZIP file. Must not be {@code null}.
     */
    private void tryDeleteLegacyAssetsZip(Path existingPackPath, Path pluginPackPath) {
        if (existingPackPath == null || !isLegacyAssetsZip(existingPackPath)) {
            return;
        }

        var pluginDir = pluginPackPath.getParent();
        var existingDir = existingPackPath.getParent();
        if (pluginDir == null || !pluginDir.equals(existingDir)) {
            return;
        }

        try {
            if (Files.deleteIfExists(existingPackPath)) {
                info("Deleted legacy assets archive " + existingPackPath + ".");
            }
        } catch (Exception ex) {
            plugin.getLogger()
                .at(Level.WARNING)
                .withCause(ex)
                .log("Failed to delete legacy assets archive " + existingPackPath + ".");
        }
    }

    /**
     * Resolves and returns the unique identifier of the asset editor pack associated with the plugin. This identifier
     * is generated using the plugin's manifest and is intended for use in operations that require a consistent and
     * recognizable pack ID.
     *
     * @return the unique identifier of the asset editor pack as a {@code String}.
     */
    private String resolvePackId() {
        return new PluginIdentifier(plugin.getManifest()).toString();
    }

    /**
     * Checks if the specified {@code Path} represents a legacy assets ZIP file. A file is considered a legacy assets
     * ZIP file if its name contains the word "assets" (case-insensitive) and ends with the ".zip" extension.
     *
     * @param path the {@code Path} to be checked. Must not be null.
     * @return {@code true} if the specified path is a legacy assets ZIP file; {@code false} otherwise.
     */
    private boolean isLegacyAssetsZip(Path path) {
        var fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".zip") && fileName.contains("assets");
    }

    /**
     * Normalizes the given {@code Path} object by converting it to an absolute path and removing any redundant
     * elements, such as "." or "..".
     *
     * @param path the {@code Path} object to normalize. Can be null.
     * @return the normalized absolute {@code Path} object, or {@code null} if the input is null.
     */
    private Path normalizePath(Path path) {
        if (path == null) {
            return null;
        }
        return path.toAbsolutePath().normalize();
    }

    /**
     * Compares two {@code Path} objects to determine if they refer to the same path.
     *
     * @param a the first {@code Path} object to compare. Can be null.
     * @param b the second {@code Path} object to compare. Can be null.
     * @return {@code true} if both {@code Path} objects are non-null and equal; {@code false} otherwise.
     */
    private boolean samePath(Path a, Path b) {
        return a != null && a.equals(b);
    }

    /**
     * Determines the desired index of an asset pack in the provided list based on the current configuration
     * and the resolved pack ID. If the pack ID matches the base asset pack ID, the current order is kept unchanged.
     * Otherwise, the index is calculated relative to the base pack's position.
     *
     * @param packs the list of {@code AssetPack} objects available for ordering. Must not be null.
     * @return the desired index of the asset pack in the list. If the base pack is not found, it returns 0.
     */
    private int desiredPackIndex(List<AssetPack> packs) {
        var packId = resolvePackId();
        var basePackId = config.baseAssetPackId();

        if (packId.equals(basePackId)) {
            debug("baseAssetPackId matches this pack id; leaving current order unchanged.");
            return indexOfPack(packs, packId);
        }

        var basePackIndex = indexOfPack(packs, basePackId);
        if (basePackIndex < 0) {
            return 0;
        }
        return basePackIndex + 1;
    }

    /**
     * Finds the index of an asset pack in the specified list by its identifier. If no matching asset pack is found,
     * returns -1.
     *
     * @param packs  the list of {@code AssetPack} objects to search through. Must not be null.
     * @param packId the identifier of the asset pack to locate. Must not be null.
     * @return the index of the asset pack in the list if found, or -1 if no matching pack exists.
     */
    private int indexOfPack(List<AssetPack> packs, String packId) {
        for (var i = 0; i < packs.size(); i++) {
            var pack = packs.get(i);
            if (pack != null && packId.equals(pack.getName())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Logs a debugging message using the plugin's logger if verbose logging is enabled in the configuration.
     *
     * @param message the debugging message to log. Must not be null.
     */
    private void debug(String message) {
        if (config.verboseLogging()) {
            plugin.getLogger().at(Level.INFO).log(message);
        }
    }

    /**
     * Logs an informational message using the plugin's logger.
     *
     * @param message the informational message to log. Must not be null.
     */
    private void info(String message) {
        plugin.getLogger().at(Level.INFO).log(message);
    }

    /**
     * Logs a warning message using the plugin's logger.
     *
     * @param message the warning message to log. Must not be null.
     */
    private void warn(String message) {
        plugin.getLogger().at(Level.WARNING).log(message);
    }
}

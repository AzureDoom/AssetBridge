package com.azuredoom.hytale.asseteditor.runtime;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.AssetPackRegisterEvent;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.lang.reflect.Method;
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

    public DefaultAssetEditorPackBridge(@Nonnull JavaPlugin plugin) {
        this(plugin, AssetEditorRuntimeConfig.defaults());
    }

    public DefaultAssetEditorPackBridge(
        @Nonnull JavaPlugin plugin,
        @Nonnull AssetEditorRuntimeConfig config
    ) {
        this.plugin = plugin;
        this.config = config;
    }

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

    @Override
    public void ensureAssetEditorPackVisible() {
        if (!config.enabled()) {
            debug("Asset editor runtime disabled; skipping AssetEditor synchronization.");
            return;
        }

        AssetModule assetModule = AssetModule.get();
        if (assetModule == null) {
            warn("Asset editor sync skipped because AssetModule is unavailable.");
            return;
        }

        String packId = resolvePackId();
        AssetPack pack = assetModule.getAssetPack(packId);
        if (pack == null) {
            warn("Asset editor sync skipped because pack '" + packId + "' was not found.");
            return;
        }

        try {
            Class<?> assetEditorPluginClass = Class.forName(ASSET_EDITOR_PLUGIN_CLASS);
            Method getMethod = assetEditorPluginClass.getMethod("get");
            Object assetEditorPlugin = getMethod.invoke(null);
            if (assetEditorPlugin == null) {
                debug("AssetEditor plugin instance unavailable.");
                return;
            }

            Method getDataSourceForPack =
                assetEditorPluginClass.getMethod("getDataSourceForPack", String.class);

            if (getDataSourceForPack.invoke(assetEditorPlugin, packId) != null) {
                debug("AssetEditor already has a data source for pack '" + packId + "'.");
                return;
            }

            Method onRegisterAssetPack =
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

    private void onEarlyAssetLoad(LoadAssetEvent event) {
        AssetModule assetModule = AssetModule.get();
        if (assetModule == null) {
            warn("Asset pack ordering skipped because AssetModule is unavailable during LoadAssetEvent.");
            return;
        }

        String packId = resolvePackId();
        Path pluginPackPath = normalizePath(plugin.getFile());

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

        List<AssetPack> packs = assetModule.getAssetPacks();
        int currentIndex = indexOfPack(packs, packId);
        if (currentIndex < 0) {
            warn("Pack '" + packId + "' was not found after registration attempt.");
            return;
        }

        int targetIndex = desiredPackIndex(packs);
        if (currentIndex == targetIndex) {
            debug("Pack '" + packId + "' is already ordered at index " + currentIndex + ".");
            return;
        }

        AssetPack pack = packs.remove(currentIndex);
        if (currentIndex < targetIndex) {
            targetIndex--;
        }
        packs.add(targetIndex, pack);

        info("Moved pack '" + packId + "' from index " + currentIndex + " to index " + targetIndex + ".");
    }

    private void removeLegacyStandaloneAssetPack(AssetModule assetModule, String packId, Path pluginPackPath) {
        String legacyPackId = packId + " (Assets)";
        AssetPack legacyPack = assetModule.getAssetPack(legacyPackId);
        if (legacyPack == null) {
            return;
        }

        Path legacyPackPath = normalizePath(legacyPack.getPackLocation());
        info("Removing legacy standalone pack '" + legacyPackId + "' from " + legacyPackPath + ".");
        assetModule.unregisterPack(legacyPackId);
        tryDeleteLegacyAssetsZip(legacyPackPath, pluginPackPath);
    }

    private void tryDeleteLegacyAssetsZip(Path existingPackPath, Path pluginPackPath) {
        if (existingPackPath == null || !isLegacyAssetsZip(existingPackPath)) {
            return;
        }

        Path pluginDir = pluginPackPath.getParent();
        Path existingDir = existingPackPath.getParent();
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

    private String resolvePackId() {
        return new PluginIdentifier(plugin.getManifest()).toString();
    }

    private boolean isLegacyAssetsZip(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".zip") && fileName.contains("assets");
    }

    private Path normalizePath(Path path) {
        if (path == null) {
            return null;
        }
        return path.toAbsolutePath().normalize();
    }

    private boolean samePath(Path a, Path b) {
        return a != null && a.equals(b);
    }

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

    private int indexOfPack(List<AssetPack> packs, String packId) {
        for (int i = 0; i < packs.size(); i++) {
            AssetPack pack = packs.get(i);
            if (pack != null && packId.equals(pack.getName())) {
                return i;
            }
        }
        return -1;
    }

    private void debug(String message) {
        if (config.verboseLogging()) {
            plugin.getLogger().at(Level.INFO).log(message);
        }
    }

    private void info(String message) {
        plugin.getLogger().at(Level.INFO).log(message);
    }

    private void warn(String message) {
        plugin.getLogger().at(Level.WARNING).log(message);
    }
}

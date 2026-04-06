package com.azuredoom.hytale.asseteditor.runtime;

/**
 * Defines a bridge interface to manage asset editor pack functionality. This interface provides methods for registering
 * hooks and ensuring the visibility of asset editor packs within the runtime environment.
 */
public interface AssetEditorPackBridge {

    /**
     * Registers a hook to ensure that asset packs can be ordered early during the runtime initialization process. This
     * method is intended to allow for custom logic that adjusts the loading priority of asset packs, ensuring they are
     * appropriately configured before other runtime operations occur.
     */
    void registerEarlyAssetPackOrderingHook();

    /**
     * Ensures that the asset editor pack is visible within the runtime environment. This method is designed to make the
     * relevant asset editor resources accessible for editing or management purposes. Its implementation typically
     * interacts with internal runtime mechanisms to validate and enforce the visibility of the asset pack.
     */
    void ensureAssetEditorPackVisible();
}

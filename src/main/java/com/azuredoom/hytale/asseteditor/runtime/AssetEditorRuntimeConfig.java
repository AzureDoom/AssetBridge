package com.azuredoom.hytale.asseteditor.runtime;

/**
 * Immutable configuration class for the Asset Editor runtime environment. This class provides a set of runtime settings
 * to control various aspects of the asset editor functionality, such as enabling/disabling features, controlling asset
 * pack ordering, and enabling verbose logging.
 */
public final class AssetEditorRuntimeConfig {

    private final boolean enabled;

    private final boolean enableEarlyAssetPackOrdering;

    private final boolean verboseLogging;

    private final String baseAssetPackId;

    private final short earlyAssetPackOrderPriority;

    /**
     * Constructs an instance of {@code AssetEditorRuntimeConfig} using the provided {@code Builder}. This constructor
     * initializes all the fields of the {@code AssetEditorRuntimeConfig} based on the values specified in the
     * {@code Builder}.
     *
     * @param builder the {@code Builder} instance containing the configuration settings for creating the
     *                {@code AssetEditorRuntimeConfig} object.
     */
    private AssetEditorRuntimeConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.enableEarlyAssetPackOrdering = builder.enableEarlyAssetPackOrdering;
        this.verboseLogging = builder.verboseLogging;
        this.baseAssetPackId = builder.baseAssetPackId;
        this.earlyAssetPackOrderPriority = builder.earlyAssetPackOrderPriority;
    }

    /**
     * Provides a default runtime configuration for the asset editor. This method initializes and returns an instance of
     * {@code AssetEditorRuntimeConfig} with pre-defined default settings.
     *
     * @return an {@code AssetEditorRuntimeConfig} object configured with default values for runtime behavior.
     */
    public static AssetEditorRuntimeConfig defaults() {
        return builder().build();
    }

    /**
     * Creates a new instance of {@code Builder} for constructing an {@code AssetEditorRuntimeConfig}. The builder
     * allows customization of various runtime settings.
     *
     * @return a new {@code Builder} instance for configuring and building an {@code AssetEditorRuntimeConfig}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Indicates whether the current configuration or feature is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Determines whether early asset pack ordering is enabled in the current configuration. This feature allows asset
     * packs to be prioritized and loaded earlier during the runtime initialization process to meet specific runtime
     * requirements.
     *
     * @return {@code true} if early asset pack ordering is enabled, {@code false} otherwise
     */
    public boolean enableEarlyAssetPackOrdering() {
        return enableEarlyAssetPackOrdering;
    }

    /**
     * Indicates whether verbose logging is enabled in the current configuration. Verbose logging provides detailed
     * output, which is useful for debugging or tracking the internal operations within the runtime environment.
     *
     * @return {@code true} if verbose logging is enabled, {@code false} otherwise
     */
    public boolean verboseLogging() {
        return verboseLogging;
    }

    /**
     * Retrieves the identifier of the base asset pack. This identifier is used to reference the primary asset pack
     * managed by the current configuration.
     *
     * @return the identifier of the base asset pack as a {@code String}.
     */
    public String baseAssetPackId() {
        return baseAssetPackId;
    }

    /**
     * Retrieves the priority value for early asset pack ordering. This priority determines the order in which asset
     * packs are processed during the runtime initialization when early asset pack ordering is enabled.
     *
     * @return the priority value for early asset pack ordering as a {@code short}.
     */
    public short earlyAssetPackOrderPriority() {
        return earlyAssetPackOrderPriority;
    }

    /**
     * A builder class for constructing instances of {@code AssetEditorRuntimeConfig}. This class provides methods to
     * customize various runtime settings, such as enabling features, configuring verbose logging, setting asset pack
     * identifiers, and prioritizing asset pack ordering.
     */
    public static final class Builder {

        private boolean enabled = true;

        private boolean enableEarlyAssetPackOrdering = true;

        private boolean verboseLogging = false;

        private String baseAssetPackId = "Hytale:Hytale";

        private short earlyAssetPackOrderPriority = (short) -40;

        private Builder() {}

        /**
         * Sets whether the feature is enabled.
         *
         * @param enabled a boolean indicating whether the feature should be enabled
         * @return the current {@code Builder} instance for method chaining
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Enables or disables early asset pack ordering during the runtime initialization process. This method allows
         * the user to specify whether asset packs should be ordered early to ensure proper prioritization and
         * configuration of assets before other runtime operations occur.
         *
         * @param enableEarlyAssetPackOrdering a boolean indicating whether early asset pack ordering should be enabled.
         *                                     If {@code true}, asset packs will be prioritized early during runtime
         *                                     initialization.
         * @return the current {@code Builder} instance for method chaining
         */
        public Builder enableEarlyAssetPackOrdering(boolean enableEarlyAssetPackOrdering) {
            this.enableEarlyAssetPackOrdering = enableEarlyAssetPackOrdering;
            return this;
        }

        /**
         * Enables or disables verbose logging for the asset editor runtime configuration. When verbose logging is
         * enabled, detailed log messages will be generated to aid in debugging and troubleshooting.
         *
         * @param verboseLogging a boolean indicating whether verbose logging should be enabled. If {@code true},
         *                       detailed logging will be activated; otherwise, logging will remain at the default
         *                       level.
         * @return the current {@code Builder} instance for method chaining.
         */
        public Builder verboseLogging(boolean verboseLogging) {
            this.verboseLogging = verboseLogging;
            return this;
        }

        /**
         * Sets the base asset pack identifier for the runtime configuration. The identifier specifies the primary asset
         * pack to be used within the runtime environment.
         *
         * @param baseAssetPackId the string representing the identifier of the base asset pack. This value must not be
         *                        null and typically follows a naming convention such as {@code namespace:packname}.
         * @return the current {@code Builder} instance for method chaining
         */
        public Builder baseAssetPackId(String baseAssetPackId) {
            this.baseAssetPackId = baseAssetPackId;
            return this;
        }

        /**
         * Sets the priority for early asset pack ordering during the runtime initialization process. This method allows
         * specifying the priority level for ordering asset packs to ensure proper prioritization and configuration
         * before other runtime operations occur.
         *
         * @param earlyAssetPackOrderPriority a short integer representing the priority level for early asset pack
         *                                    ordering. Higher values indicate greater priority.
         * @return the current {@code Builder} instance for method chaining.
         */
        public Builder earlyAssetPackOrderPriority(short earlyAssetPackOrderPriority) {
            this.earlyAssetPackOrderPriority = earlyAssetPackOrderPriority;
            return this;
        }

        /**
         * Builds a new {@code AssetEditorRuntimeConfig} instance using the current state of the {@code Builder}.
         *
         * @return a newly constructed {@code AssetEditorRuntimeConfig} instance configured with the properties
         *         specified in the {@code Builder}.
         */
        public AssetEditorRuntimeConfig build() {
            return new AssetEditorRuntimeConfig(this);
        }
    }
}

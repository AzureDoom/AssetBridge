# AssetBridge

> Runtime helper for embedding and exposing Hytale Asset Editor packs
> inside plugins.

[![Version](https://img.shields.io/badge/version-0.1.0-blue)](#)
[![Build](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![License](https://img.shields.io/badge/license-MIT-green)](#)
[![Hytale](https://img.shields.io/badge/Hytale-Plugin-orange)](#)

---

## Overview

AssetBridge is a lightweight runtime library that lets you bundle asset
packs directly inside your plugin jar and have them:

-   Properly ordered during runtime
-   Visible inside the Hytale Asset Editor

---

## Features

-   Asset Editor compatibility out of the box
-   Early asset-pack ordering support
-   Fully configurable runtime behavior
-   Plug-and-play integration

---

## Installation

### Gradle (Standard)

``` gradle
repositories {
    mavenCentral()
    maven {
        name = 'AzureDoom Maven'
        url = uri("https://maven.azuredoom.com/mods")
    }
}

dependencies {
    implementation 'com.azuredoom.hytale:hytale-asset-editor-runtime:0.1.0'
}
```

### Shade into your jar

``` gradle
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from {
        configurations.runtimeClasspath
            .filter { it.name.endsWith('.jar') }
            .collect { zipTree(it) }
    }
}
```

#### Multiple mods using AssetBridge

It is generally safe for multiple mods to bundle AssetBridge in their own jars.

To avoid conflicts:
- ensure each mod has a unique plugin identifier / asset pack ID
- prefer using the same AssetBridge version across mods
- adjust or disable early asset-pack ordering if multiple mods need custom pack positioning

---

### Using `hytale-tools` (Recommended)

``` gradle
plugins {
    id 'java'
    id 'com.azuredoom.hytale-tools' version '1.0.15'
}
```

You **do NOT need to add the repository manually**.

``` gradle
dependencies {
    implementation 'com.azuredoom.hytale:hytale-asset-editor-runtime:0.1.0'
}
```

---

## Usage

### Full Example

``` java
public final class ExamplePluginIntegration extends JavaPlugin {
    private AssetEditorPackBridge assetEditorBridge;

    @Override
    protected void setup() {
        assetEditorBridge = AssetEditorRuntime.create(
            this,
            AssetEditorRuntimeConfig.builder()
                .enabled(true)
                .enableEarlyAssetPackOrdering(true)
                .verboseLogging(false)
                .build()
        );

        assetEditorBridge.registerEarlyAssetPackOrderingHook();
    }

    @Override
    protected void start() {
        if (assetEditorBridge != null) {
            assetEditorBridge.ensureAssetEditorPackVisible();
        }
    }
}
```

---

### Minimal Setup

``` java
AssetEditorPackBridge bridge = AssetEditorRuntime.create(this);
bridge.registerEarlyAssetPackOrderingHook();
```

---

## Configuration

``` java
AssetEditorRuntimeConfig config = AssetEditorRuntimeConfig.builder()
    .enabled(true)
    .enableEarlyAssetPackOrdering(true)
    .verboseLogging(false)
    .baseAssetPackId("Hytale:Hytale")
    .earlyAssetPackOrderPriority((short) -40)
    .build();
```

### Options

| Option                         | Description                          |
|--------------------------------|--------------------------------------|
| `enabled`                      | Enable/disable runtime               |
| `enableEarlyAssetPackOrdering` | Register early load hook             |
| `verboseLogging`               | Enable debug logging                 |
| `baseAssetPackId`              | Reference pack for ordering          |
| `earlyAssetPackOrderPriority`  | Hook priority                        |

---

## How It Works

AssetBridge hooks into Hytale's asset lifecycle to:

1.  Register your embedded asset pack
2.  Replace legacy standalone packs if present
3.  Ensure correct load order
4.  Expose the pack to the Asset Editor when built.

All automatically – no manual intervention required.

---

## 🙏 Credits

Inspired by:

-   https://github.com/Alechilles/AlecsTamework
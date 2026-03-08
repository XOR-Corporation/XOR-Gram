# XORGram - Modular Overlay System for Telegram Android

<div align="center">

![XORGram Logo](docs/logo.png)

**A revolutionary modular overlay system for Telegram Android**

[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://developer.android.com/about/versions/lollipop)

[Features](#features) • [Architecture](#architecture) • [Modules](#modules) • [Building](#building) • [Contributing](#contributing)

</div>

---

## Overview

XORGram is **NOT** a traditional Telegram fork. It's a **detachable modular overlay** that sits on top of Telegram AOSP with minimal injection points (~50 hooks). This architecture solves the main pain point of all existing Telegram mods: **merge conflicts when updating from upstream**.

### Key Innovation

```
┌─────────────────────────────────────────────────────────────┐
│                      XORGram Overlay                         │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐           │
│  │xor-ghost│ │xor-vault│ │xor-sec  │ │xor-ui   │ ...       │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘           │
│       │           │           │           │                 │
│       └───────────┴─────┬─────┴───────────┘                 │
│                         │                                    │
│                   ┌─────▼─────┐                              │
│                   │ xor-bridge│  ← Single Entry Point        │
│                   └─────┬─────┘                              │
└─────────────────────────┼───────────────────────────────────┘
                          │ ~50 hooks
┌─────────────────────────▼───────────────────────────────────┐
│                   Telegram AOSP                              │
│              (Unmodified except patches)                     │
└─────────────────────────────────────────────────────────────┘
```

## Features

### 🎭 Ghost Engine (xor-ghost)
- Hide online status without appearing "last seen recently"
- Ghost mode profiles (Full, Stealth, Custom)
- Scheduled ghost mode activation
- Read receipts blocking
- Typing indicator control

### 📦 Data Vault (xor-vault)
- Anti-recall: Capture deleted messages before removal
- Edit history tracking
- Profile snapshot service
- BlackBox activity logger
- Self-destructing message capture

### 🔐 Security (xor-security)
- Biometric lock (Fingerprint/Face ID)
- Panic button for emergency wipe
- Fake PIN with decoy interface
- Client masking (hide XORGram identity)
- App lock with timeout

### 🎨 UI/UX (xor-ui)
- Custom theme engine
- Bubble customization
- Icon pack support
- Chat backgrounds
- Custom fonts

### 🎵 Media (xor-media)
- Playback speed boost (0.25x - 4x)
- Background playback
- Audio equalizer
- Video player enhancements
- Media downloader

### 🤖 AI Assistant (xor-ai)
- Message summarization
- Voice transcription (Whisper)
- Real-time translation
- Smart reply suggestions

### 📊 Admin Tools (xor-admin)
- Admin dashboard with statistics
- Auto-posting scheduler
- Bulk operations manager
- Analytics engine

### ⚙️ Automation (xor-automation)
- Rule-based automation engine
- Auto-reply system
- Scheduled actions (cron-like)
- Trigger-based actions

### 🔌 Plugins (xor-plugins)
- Python runtime support
- JavaScript runtime support
- Plugin sandboxing
- Plugin marketplace

### 🔄 Sync (xor-sync)
- P2P sync with E2E encryption
- Cloud backup
- Settings sync

## Architecture

### Module System

All modules implement the [`XORModule`](xor-core/src/main/java/org/xorgram/core/XORModule.java) interface:

```java
public interface XORModule {
    String getId();
    String getName();
    String getVersion();
    Set<XOREvent> getSubscribedEvents();
    void onInit(Context context, XORConfig config);
    boolean onEvent(XOREvent event, Object... args);
    List<XORSetting> getSettings();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    void onDestroy();
}
```

### Bridge Pattern

The [`XORBridge`](xor-bridge/src/main/java/org/xorgram/bridge/XORBridge.java) is the single entry point for all Telegram hooks:

```java
public final class XORBridge {
    // Initialization
    public static void init(Application app) { ... }
    
    // Ghost hooks
    public static boolean onBeforeMarkRead(long dialogId) { ... }
    public static boolean onBeforeSendTyping(long dialogId) { ... }
    public static boolean onBeforeUpdateOnline(boolean online) { ... }
    
    // Vault hooks
    public static void onBeforeDeleteMessages(long dialogId, List<Long> messageIds) { ... }
    public static void onMessageEdited(long dialogId, long messageId, String newText) { ... }
    
    // ... ~50 hook methods
}
```

### Event System

Modules communicate via [`XOREventBus`](xor-core/src/main/java/org/xorgram/core/XOREventBus.java):

```java
// Subscribe to events
eventBus.subscribe(event -> {
    // Handle event
});

// Post events
eventBus.post(XOREvent.MESSAGE_RECEIVED, message);
```

### Configuration

[`XORConfig`](xor-core/src/main/java/org/xorgram/core/XORConfig.java) provides encrypted, per-module settings:

```java
// Get module settings
String value = config.getString("xor-ghost", "profile_mode", "stealth");

// Set module settings
config.setBoolean("xor-ghost", "hide_typing", true);
```

## Project Structure

```
XOR-Gram/
├── xor-core/              # Core module (DI, config, events, module manager)
├── xor-bridge/            # Bridge API with hook points
├── xor-ghost/             # Ghost Engine module
├── xor-vault/             # Data Vault module
├── xor-security/          # Security module
├── xor-ui/                # UI/UX module
├── xor-media/             # Media enhancements module
├── xor-sync/              # P2P/Server sync module
├── xor-ai/                # AI Assistant module
├── xor-admin/             # Admin tools module
├── xor-automation/        # Automation engine module
├── xor-plugins/           # Plugin system module
├── scripts/               # Build and patch scripts
│   ├── apply_patches.sh   # Apply XORGram patches
│   ├── generate_patches.sh# Generate patch files
│   ├── update_upstream.sh # Update Telegram AOSP
│   └── verify_hooks.sh    # Verify hook points
└── TMessagesProj/         # Telegram AOSP (unmodified)
```

## Building

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17+
- Android SDK 34
- NDK (for native components)

### Build Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/xorgram/xorgram.git
   cd xorgram
   ```

2. **Initialize Telegram AOSP submodule:**
   ```bash
   git submodule update --init --recursive
   ```

3. **Apply XORGram patches:**
   ```bash
   ./scripts/apply_patches.sh
   ```

4. **Build with Gradle:**
   ```bash
   ./gradlew assembleRelease
   ```

### Development Build

For development, use the debug variant:

```bash
./gradlew assembleDebug
```

## Patch System

XORGram uses a minimal patch system for upstream updates:

### Applying Patches

```bash
./scripts/apply_patches.sh
```

### Generating Patches

After modifying Telegram code:

```bash
./scripts/generate_patches.sh
```

### Updating Upstream

```bash
./scripts/update_upstream.sh
```

### Verifying Hooks

```bash
./scripts/verify_hooks.sh
```

## Hook Points

XORGram injects into approximately 50 strategic points in Telegram code:

| Category | Hooks | Purpose |
|----------|-------|---------|
| Ghost | 8 | Online status, read receipts, typing |
| Vault | 12 | Message deletion, edits, profile changes |
| Security | 6 | Biometric, panic, client masking |
| UI | 10 | Themes, bubbles, icons |
| Media | 5 | Playback, equalizer, download |
| Sync | 4 | P2P, cloud backup |
| Admin | 3 | Dashboard, analytics |
| Automation | 2 | Event triggers |

## Creating a Module

1. **Create module directory:**
   ```bash
   mkdir -p xor-mymodule/src/main/java/org/xorgram/mymodule
   ```

2. **Create build.gradle:**
   ```gradle
   apply plugin: 'com.android.library'
   
   android {
       compileSdkVersion 34
       namespace 'org.xorgram.mymodule'
       // ...
   }
   
   dependencies {
       implementation project(':xor-core')
       implementation project(':xor-bridge')
   }
   ```

3. **Implement XORModule:**
   ```java
   public class MyModule implements XORModule {
       @Override
       public String getId() { return "xor-mymodule"; }
       
       @Override
       public void onInit(Context ctx, XORConfig cfg) {
           // Initialize module
       }
       
       // ... implement other methods
   }
   ```

4. **Register in settings.gradle:**
   ```gradle
   include ':xor-mymodule'
   ```

## Security Considerations

- All configuration is encrypted using AndroidX EncryptedSharedPreferences
- Plugin sandboxing prevents unauthorized access
- Client masking hides XORGram identity from Telegram servers
- Panic button for emergency data wipe
- Fake PIN provides plausible deniability

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Development Workflow

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow Android Kotlin/Java style guide
- Use meaningful variable names
- Add documentation for public APIs
- Write unit tests for new features

## License

XORGram is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## Disclaimer

This project is not affiliated with Telegram. Use at your own risk. The developers are not responsible for any consequences of using this software, including but not limited to account bans.

## Acknowledgments

- [Telegram](https://telegram.org) for the amazing messaging platform
- All contributors and testers
- The open-source community

---

<div align="center">

**Made with ❤️ by the XORGram Team**

[Website](https://xorgram.org) • [Telegram](https://t.me/xorgram) • [Twitter](https://twitter.com/xorgram)

</div>

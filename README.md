# opennextmyau

**opennextmyau** (the new OpenMyau) is a complete, 100% human-readable Java 8 source restoration of **Myau `260729`** for Minecraft `1.8.9` Forge (`11.15.1.2318-1.8.9`) + SpongePowered Mixin (`0.7.11`).

Every class, module, manager, command, utility, shader, and Mixin has been completely deobfuscated—including all methods originally compiled to x86-64 native code—alongside a full, isolated source restoration of the vendor authentication, licensing, HWID, and telemetry suite (`myau.vendor`).

> [!IMPORTANT]
> **Backup Mirror**: A self-hosted backup repository is maintained at **https://git.yxz.st/yxzero/opennextmyau** — please bookmark or save it in case Myau DMCAs this repository.

---

## Highlights

- **208 Client & Mixin Classes Fully Deobfuscated**:
  - **59 Modules** (`myau.module.modules`) with clean domain naming, structured control flow, and exact property/config compatibility (`./config/Myau/`).
  - **15 Chat Commands** (`myau.command.commands`) with tab-completion.
  - **43 Mixins & Accessors** (`myau.mixin`) with full `mcp_stable` `22-1.8.9` refmap parity.
  - **Event Bus, Managers, Utilities & GLSL Shaders** (`myau.event`, `myau.management`, `myau.util`, `myau.property`, `myau.config`).
- **Zero Native Dependencies**: All 100 native-compiled methods (`32` static initializers and `68` runtime methods) have been statically reconstructed in pure Java 8.
- **Restored Vendor & Auth Suite (`myau.vendor`)**: All 10 licensing, cipher, HWID, process-inspection, and WebSocket telemetry classes are restored in readable Java under `src/main/java/myau/vendor/` and kept completely disconnected from client execution.

---

## Protections Used & How Each Was Defeated

Myau `260729` combined **Zelix KlassMaster (ZKM)** Java obfuscation, custom **`antiLeak` JNI native transpilation**, **Themida** native binary packing, and **AES/RSA WebSocket licensing & telemetry**. Every layer was defeated purely through static analysis without executing target code:

1. **ZKM Name, Generic & Debug-Info Obfuscation**
   - **Protection**: Package hierarchies, class/field/method names, generic signatures, and local variable tables were stripped or collapsed into colliding single-letter identifiers by Zelix KlassMaster.
   - **Defeat**: Reconstructed full semantic names and generic types across all 218 classes by tracing Minecraft `1.8.9` (`mcp_stable`) interactions, Mixin target descriptors, serialized config/property keys, and module state machines.

2. **ZKM Two-Stage DES-CBC Constant & `invokedynamic` Encryption**
   - **Protection**: Over 1,580 `String`, `int`, and `long` constants across 114 classes were removed from constant pools and hidden behind `MutableCallSite` bootstrap traps backed by per-class 48-bit key schedules and two-stage `DES/CBC/NoPadding` cipher blobs.
   - **Defeat**: Reversed the 48-bit permutation and key-derivation state machine statically, decrypted every constant pool entry and string table offline, inlined the plaintext literals back into the class methods, and stripped all bootstrap decryptors.

3. **ZKM Flow-Key Opaque Predicates & Exception-Table Tangling**
   - **Protection**: Methods propagated 48-bit XOR flow keys (passed as `long` arguments or split across `(short, char, int)` and `(byte, int, int)` parameters) to drive opaque sign checks, fake jumps, and synthetic exception handlers.
   - **Defeat**: Proved the sign invariants of the derived 48-bit key slices statically, folded all opaque branches and dead exception edges, stripped every flow-key field and dummy parameter, and restructured all remaining labeled blocks into clean Java control flow.

4. **JNI Native Transpilation (`antiLeak` Native Bridge)**
   - **Protection**: 100 methods across 32 core classes—including client bootstrap (`Initializer`, `Myau`), core managers (`EventManager`, `ModuleManager`, `PropertyManager`, `CommandManager`, `BlinkManager`, `LagManager`, `Config`), and flagship modules (`KillAura`, `Scaffold`, `AutoClicker`, `BedNuker`, `LongJump`, `TargetStrafe`, `InvManager`, `BedTracker`, etc.)—were stripped of Java bytecode (`native`) and compiled into x86-64 machine code using cached `.bss` JNI class/field/method handles.
   - **Defeat**: Recovered the full `NativeMethodTable` registration map and `.bss` JNI handle cache from the native init routines, traced every `JNIEnv` call and arithmetic/branch path statically, and rewrote all 100 routines in pure Java 8.

5. **Themida Native Binary Packing (`x64-windows.dll`)**
   - **Protection**: The embedded 64-bit Windows native library was packed and protected with Themida, encrypting code/data sections and obscuring imports behind dynamic thunks.
   - **Defeat**: Statically unpacked and reconstructed the `.text`, `.data`, and `.bss` sections and resolved `JNIEnv` virtual dispatch offsets and runtime thunks offline.

6. **Encrypted Auth, HWID Fingerprinting & Runtime Telemetry**
   - **Protection**: Startup was gated by a custom loader performing JVM launch-argument anti-agent checks, SHA-256 hardware fingerprinting (CPU/MAC/disk), local account scanning, `%USERPROFILE%\client.ini` credential caching, and AES-128-ECB + RSA encrypted WebSocket authentication/heartbeats prior to native registration.
   - **Defeat**: Decoupled client startup directly into `MixinMinecraft` with all reconstructed Java logic self-contained, while restoring the entire 10-class vendor auth/telemetry implementation in `src/main/java/myau/vendor/` (unreferenced by the mod) for analysis.

---

## Project Structure

```text
src/main/java/myau/
├── Initializer.java / Myau.java   # Client bootstrap & module/command registration
├── command/                       # Command manager & 15 client commands (.bind, .config, .denick, ...)
├── config/                        # JSON config loader & saver (./config/Myau/*.json)
├── data/                          # Block, entity hitbox, packet, and vector data models
├── enums/                         # Chat color formatting & blink/lag/float module enums
├── event/                         # Reflection priority event bus & 26 client events
├── management/                    # Rotation, blink, lag, lag-range, float, friend/enemy & packet trackers
├── mixin/                         # 31 SpongePowered Mixins & 12 Accessor/Invoker interfaces
├── module/                        # Base Module, ModuleManager & all 59 modules
├── property/                      # Typed module properties (Boolean, Mode, Int, Float, Percent, Color, Text)
├── util/                          # Movement, rotation, player, item, block, team, packet, render & GLSL shaders
└── vendor/                        # Restored (unwired) vendor loader, cipher, HWID, UI & WebSocket auth classes
```

---

## Building

Requires a JDK 17+ host runtime to run Gradle/Loom (automatically compiles against a Java 8 toolchain):

```powershell
.\gradlew.bat clean build -x test
```

The remapped Forge `1.8.9` mod JAR is written to `build/libs/Myau-260729.jar`.

# opennextmyau

**opennextmyau** (the new OpenMyau) is a complete, 100% human-readable Java 8 source restoration of **Myau `260729`** for Minecraft `1.8.9` Forge (`11.15.1.2318-1.8.9`) + SpongePowered Mixin (`0.7.11`).

Every class, module, manager, command, utility, shader, and Mixin has been completely deobfuscated—including all methods originally compiled to x86-64 native code—alongside a full, isolated source restoration of the vendor authentication, licensing, HWID, and telemetry suite (`myau.vendor`).

> [!IMPORTANT]
> **Backup Mirror**: A self-hosted backup repository is maintained at **https://deobf.su/yxzero/opennextmyau** — please bookmark or save it in case Myau DMCAs this repository.

---

## Highlights

- **208 Client & Mixin Classes Fully Deobfuscated**:
  - **59 Modules** (`myau.module.modules`) with clean domain naming, structured control flow, and exact property/config compatibility (`./config/Myau/`).
  - **15 Chat Commands** (`myau.command.commands`) with tab-completion.
  - **43 Mixins & Accessors** (`myau.mixin`) with full `mcp_stable` `22-1.8.9` refmap parity.
  - **Event Bus, Managers, Utilities & GLSL Shaders** (`myau.event`, `myau.management`, `myau.util`, `myau.property`, `myau.config`).
- **Zero Native Dependencies**: All 100 native-compiled methods (`32` static initializers and `68` runtime methods) have been statically reconstructed in pure Java 8.
- **Restored Vendor & Auth Suite (`myau.vendor`)**: All 11 loader, `MethodHandle` bootstrap bridge, XXTEA packet cipher, MD5 HWID, INI store, HTTP registration, process-inspection, QQ scanner, UI, and WebSocket auth classes are restored in readable Java under `src/main/java/myau/vendor/` and kept completely disconnected from client execution.

---

## Protections Used & How Each Was Defeated

Myau `260729` combined **Zelix KlassMaster (ZKM)** Java obfuscation, custom **`antiLeak` JNI native transpilation**, **VMProtect 3.x virtualization + Themida 3.x binary packing**, and **custom-delta XXTEA WebSocket licensing & host telemetry**. Every layer was defeated purely through static analysis and offline CPU emulation without executing target binaries on the host:

1. **ZKM Name, Generic & Debug-Info Obfuscation**
   - **Protection**: Package hierarchies, class/field/method names, generic signatures, and local variable tables were stripped or collapsed into colliding single-letter identifiers by Zelix KlassMaster.
   - **Defeat**: Reconstructed full semantic names and generic types across all 219 restored classes (`208` client/Mixin classes + `11` vendor classes) by tracing Minecraft `1.8.9` (`mcp_stable`) interactions, Mixin target descriptors, serialized config/property keys, and module state machines.

2. **ZKM Two-Stage DES-CBC Constant & `invokedynamic` Encryption**
   - **Protection**: Over 1,580 `String`, `int`, and `long` constants across 114 classes were removed from constant pools and hidden behind `MutableCallSite` bootstrap traps backed by per-class 48-bit key schedules and two-stage `DES/CBC/NoPadding` cipher blobs.
   - **Defeat**: Reversed the 48-bit permutation and key-derivation state machine statically, decrypted every constant pool entry and string table offline, inlined the plaintext literals back into the class methods, and stripped all bootstrap decryptors.

3. **ZKM Flow-Key Opaque Predicates & Exception-Table Tangling**
   - **Protection**: Methods propagated 48-bit XOR flow keys (passed as `long` arguments or split across `(short, char, int)` and `(byte, int, int)` parameters) to drive opaque sign checks, fake jumps, and synthetic exception handlers.
   - **Defeat**: Proved the sign invariants of the derived 48-bit key slices statically, folded all opaque branches and dead exception edges, stripped every flow-key field and dummy parameter, and restructured all remaining labeled blocks into clean Java control flow.

4. **JNI Native Transpilation (`antiLeak` Native Bridge & `Bootstrap0`)**
   - **Protection**: 100 methods across 32 core classes—including client bootstrap (`Initializer`, `Myau`), core managers (`EventManager`, `ModuleManager`, `PropertyManager`, `CommandManager`, `BlinkManager`, `LagManager`, `Config`), and flagship modules (`KillAura`, `Scaffold`, `AutoClicker`, `BedNuker`, `LongJump`, `TargetStrafe`, `InvManager`, `BedTracker`, etc.)—were stripped of Java bytecode (`native`) and compiled into x86-64 machine code registered via `Loader.registerNativesForClass(int, Class<?>)` alongside an embedded 2,192-byte `Bootstrap0` `MethodHandle` reverse-invocation bridge class (`invokereverse0`..`invokereverse4`).
   - **Defeat**: Recovered the full `NativeMethodTable` registration map, embedded `Bootstrap0.class` payload, and `.bss` JNI handle cache from the native init routines, traced every `JNIEnv` call and arithmetic/branch path statically, and rewrote all 100 routines in pure Java 8.

5. **VMProtect 3.x Virtualization & Themida 3.x Packing (`x64-windows.dll`)**
   - **Protection**: Critical native entrypoints (`JNI_OnLoad`, the WebSocket session dispatcher `WS_Message_Callback`, and native registration bridges) were virtualized with **VMProtect 3.x** (rolling-key bytecode handlers with full `.text` integrity CRC loops) and outer-packed with **Themida 3.x** (blanked 8-space PE section headers, `.boot` unpacker stub, and 6-byte encrypted IAT thunks).
   - **Defeat**: Statically unpacked and reconstructed `.text`, `.rdata`, `.data`, and VM bytecode sections, lifted all 2,781 virtualized basic blocks of `WS_Message_Callback` and `JNI_OnLoad`, and resolved all `JNIEnv` dispatch slots and encrypted import thunks offline.

6. **XXTEA WebSocket Auth, MD5 HWID Fingerprinting & Host Telemetry**
   - **Protection**: Startup was gated by a Dear ImGui (`1.88 WIP`) + DirectX 11 login/registration overlay (`Powered by 苏辰`) performing `-xbootclasspath` and `-javaagent:` JVM command-line inspection (`nlohmann/json 3.11.2`), local Tencent QQ UIN scanning (`\d{5,10}` across NT QQ `Documents\Tencent Files\nt_qq\global\nt_data\Login` and legacy `\Tencent\Users\*.*`), `%USERPROFILE%\myau.ini` credential caching (`CSimpleIniA`), plain-HTTP account registration (`POST http://foodbyte.cc/api/register` with `User-Agent: FSMClient` and `Client: myau`), 32-character uppercase **MD5** hardware fingerprinting (`CPUID(0x80000002)` signed decimal registers + unsigned decimal `C:\` volume serial + NetBIOS computer name + `CPUID(0)` CPU vendor), and **custom-delta XXTEA** (`DELTA = 0x9E3779B1`, seeded with `"中国"` as initial `clientKey`/`serverKey`) over `wss://foodbyte.cc:443/MYAU/<handshake_hex>` using `$`-delimited command frames (`setClientKey`, `setServerKey`, `begin`, `notify`, `download`, `printString`, `setEnvString`, `getInfo`/`InfoTag`, `getHwid`, `hwAuth`, `fail`, `exit`) and unencrypted `{ "@class" : ".pong" }` heartbeats.
   - **Defeat**: Decoupled client startup directly into `MixinMinecraft` with all reconstructed Java logic self-contained, while restoring the complete 11-class vendor auth/telemetry implementation in `src/main/java/myau/vendor/` (unreferenced by the mod) for analysis.

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
└── vendor/                        # Restored (unwired) 11-class loader, Bootstrap0, XXTEA, MD5 HWID, UI & WebSocket auth
```

---

## Building

Requires a JDK 17+ host runtime to run Gradle/Loom (automatically compiles against a Java 8 toolchain):

```powershell
.\gradlew.bat clean build -x test
```

The remapped Forge `1.8.9` mod JAR is written to `build/libs/Myau-260729.jar`.

# LoggerHookAndroid
Hook into any app and log its behavior.

Minimal Kotlin Android hook-runtime APK source.

## Build

```bash
./BUILD_WITH_SYSTEM_GRADLE.sh
```

APK output:

```bash
app/build/outputs/apk/debug/app-debug.apk
```

## Static smali-friendly methods

```smali
invoke-static {v0}, Lcom/dct/hooklogger/Hook;->log(Ljava/lang/String;)V
invoke-static {v0, v1}, Lcom/dct/hooklogger/Hook;->log(Ljava/lang/String;Ljava/lang/String;)V
invoke-static {v0, v1}, Lcom/dct/hooklogger/Hook;->kv(Ljava/lang/String;Ljava/lang/Object;)V
invoke-static {v0}, Lcom/dct/hooklogger/Hook;->trace(Ljava/lang/String;)V
invoke-static {v0}, Lcom/dct/hooklogger/Hook;->stack(Ljava/lang/String;)V
invoke-static {v0, v1}, Lcom/dct/hooklogger/Hook;->hex(Ljava/lang/String;[B)V
invoke-static {v0, v1}, Lcom/dct/hooklogger/Hook;->dumpObj(Ljava/lang/String;Ljava/lang/Object;)V
invoke-static {v0, v1}, Lcom/dct/hooklogger/Hook;->bundle(Ljava/lang/String;Landroid/os/Bundle;)V
invoke-static {}, Lcom/dct/hooklogger/Hook;->thread()V
invoke-static {v0}, Lcom/dct/hooklogger/Hook;->init(Landroid/content/Context;)V
```

## Bypassing Runtime Protections

This logger includes methods to help evade anti-tampering and runtime protections when reverse engineering:

1. **Disable Logcat Logging:**
   Some protections check `logcat` for tampering flags or hook standard Android logging. You can disable standard Logcat output and strictly write to the local file:
   ```smali
   invoke-static {}, Lcom/dct/hooklogger/Hook;->disableLogcat()V
   ```

2. **Prevent App Crash:**
   Some protections purposely throw uncaught exceptions to crash the app if they detect modifications. Suppress these:
   ```smali
   invoke-static {}, Lcom/dct/hooklogger/Hook;->suppressCrashes()V
   ```

3. **Bypass System.exit():**
   If an app tries to kill itself (e.g., `System.exit(0)`), find that Smali code and replace `invoke-static ..., Ljava/lang/System;->exit(I)V` with:
   ```smali
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->dummyExit(I)V
   ```

4. **Spoof Debugger Status:**
   Replace `invoke-static {}, Landroid/os/Debug;->isDebuggerConnected()Z` with:
   ```smali
   invoke-static {}, Lcom/dct/hooklogger/Hook;->fakeIsDebuggerConnected()Z
   ```

5. **Spoof Installer Source:**
   Replace `invoke-virtual {pm, pkg}, Landroid/content/pm/PackageManager;->getInstallerPackageName(Ljava/lang/String;)Ljava/lang/String;` with:
   ```smali
   # IMPORTANT: pass the original package-name register (`pkg`), not the PackageManager register (`pm`).
   # If original args are {v2, v5} where v2=pm and v5=pkg, use v5 below:
   invoke-static {v5}, Lcom/dct/hooklogger/Hook;->fakeGetInstallerPackageName(Ljava/lang/String;)Ljava/lang/String;
   ```

6. **App Tamper / Code Integrity / DEX Checksum checks:**
   - Compute SHA-256 for `classes.dex` inside an APK:
   ```smali
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->sha256ClassesDexFromApk(Ljava/lang/String;)Ljava/lang/String;
   ```
   - Compare expected/actual hash:
   ```smali
   invoke-static {v1, v2}, Lcom/dct/hooklogger/Hook;->verifySha256(Ljava/lang/String;Ljava/lang/String;)Z
   ```
   - Read legacy `PackageManager.getPackageInfo(...).signatures` values as strings:
   ```smali
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->packageSignatures(Ljava/lang/String;)[Ljava/lang/String;
   ```
   - Verify app signature by SHA-256:
   ```smali
   invoke-static {v0, v1}, Lcom/dct/hooklogger/Hook;->verifyPackageSignatureSha256(Ljava/lang/String;Ljava/lang/String;)Z
   ```


7. **Bypass Root / Magisk / SU checks:**
   - For `Runtime.exec(Ljava/lang/String;)Ljava/lang/Process;`, sanitize **then** call `exec` (do not replace `exec` directly with this helper):
   ```smali
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->sanitizedRuntimeCommand(Ljava/lang/String;)Ljava/lang/String;
   move-result-object v0
   invoke-virtual {vRuntime, v0}, Ljava/lang/Runtime;->exec(Ljava/lang/String;)Ljava/lang/Process;
   ```
   - For `Runtime.exec([Ljava/lang/String;)Ljava/lang/Process;`, sanitize argv first and keep argument semantics:
   ```smali
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->sanitizedRuntimeCommandArgs([Ljava/lang/String;)[Ljava/lang/String;
   move-result-object v0
   invoke-virtual {vRuntime, v0}, Ljava/lang/Runtime;->exec([Ljava/lang/String;)Ljava/lang/Process;
   ```
   `sanitizedRuntimeCommand*` only swaps suspicious executable tokens (e.g. `su`) while preserving the original argument tail/vector.
   - Replace file existence probes like `/system/bin/su` with:
   ```smali
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->fakeFileExistsForRoot(Ljava/lang/String;)Z
   ```
   - Replace `/proc/mounts` string checks with:
   ```smali
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->sanitizedProcMounts(Ljava/lang/String;)Ljava/lang/String;
   ```
   - Replace `SystemProperties.get(...)` result handling with:
   ```smali
   invoke-static {v0, v1}, Lcom/dct/hooklogger/Hook;->sanitizedSystemProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
   ```
   - Replace RootBeer/native detector boolean returns with:
   ```smali
   invoke-static {v0, v1}, Lcom/dct/hooklogger/Hook;->sanitizeRootBeerCheck(Ljava/lang/String;Z)Z
   ```

8. **Bypass Emulator / Virtual Device checks:**
   - Generic boolean emulator probes:
   ```smali
   invoke-static {v0, v1}, Lcom/dct/hooklogger/Hook;->sanitizeEmulatorCheck(Ljava/lang/String;Z)Z
   ```
   - Build/prop-based probes (`ro.hardware`, `ro.kernel.qemu`, `Build.FINGERPRINT`):
   ```smali
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->sanitizedHardware(Ljava/lang/String;)Ljava/lang/String;
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->sanitizedKernelQemu(Ljava/lang/String;)Ljava/lang/String;
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->sanitizedFingerprint(Ljava/lang/String;)Ljava/lang/String;
   ```
   - Telephony null/empty IMEI probes:
   ```smali
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->sanitizedImei(Ljava/lang/String;)Ljava/lang/String;
   ```
   - Sensor count and battery-level heuristics:
   ```smali
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->sanitizedSensorCount(I)I
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->sanitizedBatteryLevel(I)I
   ```

## Log location

Default log path after `Hook.init(context)`:

```text
/storage/emulated/0/Android/data/com.dct.hooklogger/files/dct_hook.log
```

When merged into another app, the package path becomes the host app package.

## Notes

- `getExternalFilesDir()` does not need runtime storage permission.
- Legacy storage permissions are included for old Android versions.
- All hook methods are `@JvmStatic` and crash-safe.

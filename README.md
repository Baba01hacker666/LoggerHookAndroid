# LoggerHookAndroid
Hook into any app to log its working 

Minimal Kotlin Android hook-runtime APK source.

## Build

```bash
./gradlew assembleDebug
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
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->fakeGetInstallerPackageName(Ljava/lang/String;)Ljava/lang/String;
   ```


6. **Bypass Root / Magisk / SU checks:**
   - Replace `Runtime.exec("su")` style calls with:
   ```smali
   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->sanitizedRuntimeCommand(Ljava/lang/String;)Ljava/lang/String;
   ```
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

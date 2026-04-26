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
invoke-static {v0}, Lcom/dct/hooklogger/Hook;->init(Landroid/content/Context;)V
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

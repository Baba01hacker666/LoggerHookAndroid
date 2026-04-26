package com.dct.hooklogger

import android.app.Application

class HookApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Hook.init(this)
        Hook.log("INIT", "DCT Hook Logger initialized")
    }
}


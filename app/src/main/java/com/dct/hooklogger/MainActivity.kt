package com.dct.hooklogger

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Hook.init(this)
        Hook.log("UI", "MainActivity opened")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            textSize = 20f
            text = "DCT Hook Logger"
        }

        val path = TextView(this).apply {
            textSize = 14f
            text = "Log file:\n${Hook.logPath()}"
        }

        root.addView(title)
        root.addView(path)
        setContentView(root)
    }
}

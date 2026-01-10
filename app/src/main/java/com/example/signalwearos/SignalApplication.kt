package com.example.signalwearos

import android.app.Application
import android.content.Context
// import androidx.multidex.MultiDex // Removed

class SignalApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // MultiDex.install(this) // Removed
    }
}

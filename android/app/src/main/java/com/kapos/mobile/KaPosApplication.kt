package com.kapos.mobile

import android.app.Application

class KaPosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: KaPosApplication
            private set
    }
}

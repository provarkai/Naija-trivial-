package com.ai4biz.app

import android.app.Application
import com.google.android.gms.ads.MobileAds

class Ai4bizApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        container = AppContainer(this)
    }
}

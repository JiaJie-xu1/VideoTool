package com.partner.videotools

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.blankj.utilcode.util.Utils

class VideoApplication : Application(){

    override fun onCreate() {
        super.onCreate()
        Utils.init(this)

    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
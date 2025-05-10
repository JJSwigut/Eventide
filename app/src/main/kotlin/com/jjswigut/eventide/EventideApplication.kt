package com.jjswigut.eventide

import android.app.Application
import com.jjswigut.eventide.di.initKoin

class EventideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(this)
    }
}

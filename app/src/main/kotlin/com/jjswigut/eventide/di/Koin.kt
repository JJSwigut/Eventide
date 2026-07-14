package com.jjswigut.eventide.di

import android.app.Application
import com.jjswigut.eventide.db.dbModule
import com.jjswigut.eventide.map.mapModule
import com.jjswigut.eventide.network.networkModule
import com.jjswigut.eventide.repository.repositoryModule
import com.jjswigut.eventide.settings.settingsModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

fun initKoin(app: Application): KoinApplication {
    return startKoin {
        androidContext(app)
        modules(
            coreModule,
            mapModule(app.applicationContext),
            dbModule,
            networkModule,
            repositoryModule,
            settingsModule,
        )
    }
}

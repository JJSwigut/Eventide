package com.jjswigut.eventide.settings

import org.koin.dsl.module

val settingsModule = module {
    single<SettingsRepository> {
        SettingsRepositoryImpl(context = get())
    }
}

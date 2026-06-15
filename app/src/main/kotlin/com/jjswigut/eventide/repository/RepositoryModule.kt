package com.jjswigut.eventide.repository

import org.koin.dsl.module

val repositoryModule = module {
    single<NoaaRepository> {
        NoaaRepositoryImpl(
            noaaService = get(),
            weatherService = get(),
            stationsDb = get(),
        )
    }
    single<FavoritesRepository> {
        FavoritesRepositoryImpl(
            stationsDb = get(),
            dispatcher = get(),
        )
    }
    single<TideAlertRepository> {
        TideAlertRepositoryImpl(
            stationsDb = get(),
            dispatcher = get(),
        )
    }
}

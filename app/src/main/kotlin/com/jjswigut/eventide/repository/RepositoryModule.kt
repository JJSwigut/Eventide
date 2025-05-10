package com.jjswigut.eventide.repository

import org.koin.dsl.module

val repositoryModule = module {
    single<NoaaRepository> {
        NoaaRepositoryImpl(
            noaaService = get(),
            stationsDb = get()
        )
    }
}
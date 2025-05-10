package com.jjswigut.eventide.network

import com.jjswigut.eventide.network.client.NoaaServiceClient
import com.jjswigut.eventide.network.service.NoaaService
import com.jjswigut.eventide.network.service.NoaaServiceImpl
import io.ktor.client.engine.android.Android
import org.koin.dsl.module

val networkModule = module {
    single<NoaaService> {
        NoaaServiceImpl(
            client = NoaaServiceClient(Android.create {
                connectTimeout = timeout
                socketTimeout = timeout
            })
        )
    }
}

private const val timeout = 60_000
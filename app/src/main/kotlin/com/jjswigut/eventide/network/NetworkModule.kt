package com.jjswigut.eventide.network

import com.jjswigut.eventide.network.client.NoaaServiceClient
import com.jjswigut.eventide.network.client.WeatherServiceClient
import com.jjswigut.eventide.network.service.NoaaService
import com.jjswigut.eventide.network.service.NoaaServiceImpl
import com.jjswigut.eventide.network.service.WeatherService
import com.jjswigut.eventide.network.service.WeatherServiceImpl
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val networkModule = module {
    single<NoaaService> {
        NoaaServiceImpl(
            client = NoaaServiceClient(
                OkHttp.create {
                    config {
                        connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
                        readTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
                        writeTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
                    }
                }
            )
        )
    }

  single<WeatherService> {
    WeatherServiceImpl(
      client = WeatherServiceClient(
        OkHttp.create {
          config {
            connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
            readTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
            writeTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
          }
        }
      )
    )
  }
}

private const val timeout = 60_000

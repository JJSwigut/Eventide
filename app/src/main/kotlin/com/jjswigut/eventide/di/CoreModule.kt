package com.jjswigut.eventide.di

import com.jjswigut.eventide.dispatchers.Dispatcher
import com.jjswigut.stonks.dispatchers.DispatcherImpl
import org.koin.dsl.module

val coreModule = module {
    single<Dispatcher> {
        DispatcherImpl()
    }
}
package com.jjswigut.eventide.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.jjswigut.eventide.StationsDb
import org.koin.dsl.module

val dbModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = StationsDb.Schema,
            context = get(),
            name = "Stations.db"
        )
    }
    single {
        StationsDb(get())
    }
}
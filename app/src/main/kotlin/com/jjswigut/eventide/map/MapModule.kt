package com.jjswigut.eventide.map

import android.content.Context
import com.google.android.gms.location.LocationServices
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

fun mapModule(context: Context) = module {
    single { LocationServices.getFusedLocationProviderClient(context) }
    viewModel {
        MapViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
}

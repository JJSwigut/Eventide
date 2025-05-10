package com.jjswigut.stonks.dispatchers

import com.jjswigut.eventide.dispatchers.Dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Dispatcher implementation that manages different types of Coroutines Dispatchers.
 *
 * Implements the Dispatcher interface.
 */

class DispatcherImpl : Dispatcher {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}

package com.jjswigut.eventide.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

/**
 * An interface for classes that need to provide different types of CoroutineDispatchers.
 */
interface Dispatcher {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

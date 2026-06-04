package com.jjswigut.eventide.network.utils

/**
 * A sealed class that encapsulates either a successful result or a failure.
 * This class has two subclasses, [Success] and [Failure], that represent each case respectively.
 *
 * There are a lot of classes like this floating around the net but I like this one the best and wrote
 * the extension functions together with a friend that likes to use them as well.
 *
 * @param S The type of the successful result.
 * @param F The type of the failure.
 */
sealed class Either<out S, out F> {
    companion object {
        fun <S> success(value: S) = Success(value)

        fun <F> failure(error: F) = Failure(error)
    }

    data class Success<out S>(val value: S) : Either<S, Nothing>()

    data class Failure<out F>(val error: F) : Either<Nothing, F>()
}

/**
 * Processes the success value of an [Either] with a transformation
 * function. The failure value is retained.
 *
 * @param success: The transformation to run on a successful value
 * @return A new [Either] constructed with the transformed success value
 */
inline fun <S1 : Any, F1 : Any, S2 : Any> Either<S1, F1>.processSuccess(
    crossinline success: (S1) -> S2,
): Either<S2, F1> {
    return when (this) {
        is Either.Success -> {
            Either.success(success(this.value))
        }
        is Either.Failure -> {
            this
        }
    }
}

/**
 * Processes the [Either] and provides callbacks with
 * values based on it's state
 *
 * @param onSuccess: callback to invoke when successful
 * @param onError: callback to invoke for an error
 */

suspend fun <S, F> Either<S, F>.process(
    onSuccess: suspend (value: S) -> Unit = {},
    onError: suspend (error: F) -> Unit = {},
) {
    when (this) {
        is Either.Success -> onSuccess.invoke(value)
        is Either.Failure -> onError.invoke(error)
    }
}

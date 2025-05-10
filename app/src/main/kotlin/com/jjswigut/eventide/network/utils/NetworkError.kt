package com.jjswigut.eventide.network.utils

import android.content.Context
import com.jjswigut.eventide.R
import com.jjswigut.eventide.utils.GenericError

data class NetworkError(
    val code: Int,
) : GenericError {
    override fun getDisplayMessage(context: Context): String {
        // todo add specific code error messages
        return context.getString(R.string.generic_network_error_text)
    }
}

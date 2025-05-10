package com.jjswigut.eventide.utils

import android.content.Context
import com.jjswigut.eventide.R

interface GenericError {
    fun getDisplayMessage(context: Context): String
}

class UnknownError() : GenericError {
    override fun getDisplayMessage(context: Context): String {
        return context.getString(R.string.generic_error_text)
    }
}

class DbError() : GenericError {
    override fun getDisplayMessage(context: Context): String {
        return context.getString(R.string.db_error_text)
    }
}
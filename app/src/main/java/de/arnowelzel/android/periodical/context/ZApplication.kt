package de.arnowelzel.android.periodical.context

import android.app.Application
import android.content.Context

class ZApplication: Application() {
    companion object {
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        ZApplication.context = applicationContext
    }
}
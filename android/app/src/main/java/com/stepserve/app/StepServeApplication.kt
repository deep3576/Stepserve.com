package com.stepserve.app

import android.app.Application
import com.stepserve.app.data.auth.TokenManager

class StepServeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
    }
}

package com.example.p2pchat

import android.app.Application
import com.example.p2pchat.core.network.MessageProcessor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class P2PChatApp : Application() {

    @Inject
    lateinit var messageProcessor: MessageProcessor

    override fun onCreate() {
        super.onCreate()
        messageProcessor.start()
    }
}

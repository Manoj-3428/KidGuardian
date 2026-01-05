package com.example.phonelock

import android.app.Application
import com.google.firebase.FirebaseApp

class PhoneLockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}


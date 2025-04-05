package com.yourstore.app

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.yourstore.app.data.source.local.PreferencesManager
import com.yourstore.app.di.appModule
import com.yourstore.app.di.dataModule
import com.yourstore.app.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MyApplication : Application() {

    companion object {
        private const val TAG = "MyApplication"
        lateinit var instance: MyApplication
            private set
    }

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Firebase
        initializeFirebase()

        // Initialize Koin for Dependency Injection
        initializeKoin()

        // Initialize Preferences
        preferencesManager = PreferencesManager(this)

        // Set up theme based on saved preference
        setupTheme()

        // Setup crash reporting
        setupCrashReporting()

        Log.d(TAG, "Application initialized")
    }

    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)

            // Configure Firestore for offline persistence
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()

            FirebaseFirestore.getInstance().firestoreSettings = settings

            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun initializeKoin() {
        startKoin {
            androidLogger(Level.ERROR) // Using ERROR level to avoid Koin issues with Kotlin 1.4+
            androidContext(this@MyApplication)
            modules(listOf(appModule, dataModule, viewModelModule))
        }
        Log.d(TAG, "Koin initialized")
    }

    private fun setupTheme() {
        val isDarkMode = preferencesManager.getDarkMode()
        val mode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        Log.d(TAG, "Theme setup completed. Dark mode: $isDarkMode")
    }

    private fun setupCrashReporting() {
        // Only collect crash reports if user has opted in
        val collectCrashReports = preferencesManager.getCrashReportingEnabled()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(collectCrashReports)

        // Add user ID to crash reports if user is logged in
        FirebaseAuth.getInstance().currentUser?.let { user ->
            FirebaseCrashlytics.getInstance().setUserId(user.uid)
        }

        Log.d(TAG, "Crash reporting setup completed")
    }
}
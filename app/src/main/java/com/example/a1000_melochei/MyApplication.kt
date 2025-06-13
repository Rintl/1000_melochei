package com.example.a1000_melochei

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.a1000_melochei.di.allModules
import com.example.a1000_melochei.util.Constants
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Основной класс приложения.
 * Инициализирует Koin DI и настраивает каналы уведомлений.
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Инициализация Koin
        initKoin()

        // Создание каналов уведомлений
        createNotificationChannels()
    }

    /**
     * Инициализация Koin Dependency Injection
     */
    private fun initKoin() {
        startKoin {
            // Логирование только в debug режиме
            if (BuildConfig.DEBUG) {
                androidLogger(Level.DEBUG)
            }

            // Контекст Android
            androidContext(this@MyApplication)

            // Модули зависимостей
            modules(allModules)
        }
    }

    /**
     * Создание каналов уведомлений для Android 8.0+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Канал для заказов
            val orderChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ORDER_ID,
                getString(R.string.notification_channel_orders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_orders_description)
                enableLights(true)
                enableVibration(true)
            }

            // Канал для промо-акций
            val promoChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_PROMO_ID,
                getString(R.string.notification_channel_promo_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_promo_description)
                enableLights(true)
                enableVibration(false)
            }

            // Канал для системных уведомлений
            val systemChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_SYSTEM_ID,
                getString(R.string.notification_channel_system_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_system_description)
                enableLights(false)
                enableVibration(false)
            }

            // Регистрация каналов
            notificationManager.createNotificationChannels(
                listOf(orderChannel, promoChannel, systemChannel)
            )
        }
    }
}
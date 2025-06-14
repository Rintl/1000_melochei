package com.example.a1000_melochei.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.a1000_melochei.R

/**
 * Вспомогательный класс для работы с уведомлениями
 */
object NotificationHelper {

    /**
     * Создает каналы уведомлений для Android 8.0 (API level 26) и выше
     * @param context Контекст приложения
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Канал для уведомлений о заказах
            val orderChannelId = context.getString(R.string.notification_channel_order_id)
            val orderChannel = NotificationChannel(
                orderChannelId,
                context.getString(R.string.notification_channel_orders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_orders_description)
            }

            // Канал для уведомлений о промо-акциях
            val promoChannelId = context.getString(R.string.notification_channel_promo_id)
            val promoChannel = NotificationChannel(
                promoChannelId,
                context.getString(R.string.notification_channel_promo),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_promo_description)
            }

            // Канал для системных уведомлений
            val systemChannelId = context.getString(R.string.notification_channel_system_id)
            val systemChannel = NotificationChannel(
                systemChannelId,
                context.getString(R.string.notification_channel_system),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_system_description)
            }

            // Регистрируем каналы
            notificationManager.createNotificationChannels(listOf(orderChannel, promoChannel, systemChannel))
        }
    }

    /**
     * Проверяет, разрешены ли уведомления в системе
     * @param context Контекст приложения
     * @return true, если уведомления разрешены
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
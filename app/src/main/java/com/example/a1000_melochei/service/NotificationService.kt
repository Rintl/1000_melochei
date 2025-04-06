package com.yourstore.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yourstore.app.R
import com.yourstore.app.ui.MainActivity
import com.yourstore.app.ui.customer.CustomerActivity
import com.yourstore.app.ui.customer.orders.OrderDetailActivity
import com.yourstore.app.util.Constants

/**
 * Сервис для работы с уведомлениями в приложении.
 * Позволяет создавать и показывать различные типы уведомлений для пользователей
 * и администраторов.
 */
class NotificationService(private val context: Context) {

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    init {
        createNotificationChannels()
    }

    /**
     * Создает каналы уведомлений для Android 8.0 (API level 26) и выше
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Канал для уведомлений о заказах
            val orderChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ORDER_ID,
                context.getString(R.string.notification_channel_orders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_orders_description)
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }

            // Канал для уведомлений о промо-акциях
            val promoChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_PROMO_ID,
                context.getString(R.string.notification_channel_promo),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_promo_description)
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
            }

            // Канал для технических уведомлений
            val systemChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_SYSTEM_ID,
                context.getString(R.string.notification_channel_system),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_system_description)
                enableLights(false)
                enableVibration(false)
            }

            // Получаем системный NotificationManager и регистрируем каналы
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(orderChannel, promoChannel, systemChannel))
        }
    }

    /**
     * Показывает уведомление о новом заказе (для администраторов)
     *
     * @param orderId ID заказа
     * @param orderNumber номер заказа
     * @param customerName имя клиента
     */
    fun showNewOrderNotification(orderId: String, orderNumber: String, customerName: String) {
        // Создаем интент для перехода к деталям заказа
        val intent = Intent(context, com.yourstore.app.ui.admin.orders.OrderDetailAdminActivity::class.java).apply {
            putExtra("ORDER_ID", orderId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Настраиваем уведомление
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ORDER_ID)
            .setSmallIcon(R.drawable.ic_notification_order)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_logo))
            .setContentTitle(context.getString(R.string.notification_new_order_title))
            .setContentText(context.getString(R.string.notification_new_order_text, orderNumber, customerName))
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        // Показываем уведомление
        showNotification(Constants.NOTIFICATION_NEW_ORDER_ID, notification)
    }

    /**
     * Показывает уведомление об обновлении статуса заказа (для клиентов)
     *
     * @param orderId ID заказа
     * @param orderNumber номер заказа
     * @param newStatus новый статус заказа
     */
    fun showOrderStatusUpdateNotification(orderId: String, orderNumber: String, newStatus: String) {
        // Создаем интент для перехода к деталям заказа
        val intent = Intent(context, OrderDetailActivity::class.java).apply {
            putExtra("ORDER_ID", orderId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Настраиваем уведомление
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ORDER_ID)
            .setSmallIcon(R.drawable.ic_notification_order)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_logo))
            .setContentTitle(context.getString(R.string.notification_order_status_title))
            .setContentText(context.getString(R.string.notification_order_status_text, orderNumber, newStatus))
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        // Показываем уведомление
        showNotification(Constants.NOTIFICATION_ORDER_STATUS_ID, notification)
    }

    /**
     * Показывает уведомление о промо-акции
     *
     * @param title заголовок акции
     * @param message описание акции
     */
    fun showPromoNotification(title: String, message: String) {
        // Создаем интент для перехода на главный экран приложения
        val intent = Intent(context, CustomerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Настраиваем уведомление
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_PROMO_ID)
            .setSmallIcon(R.drawable.ic_notification_promo)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_logo))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        // Показываем уведомление с уникальным ID для каждой акции
        showNotification(Constants.NOTIFICATION_PROMO_ID + System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Показывает системное уведомление
     *
     * @param title заголовок уведомления
     * @param message текст уведомления
     */
    fun showSystemNotification(title: String, message: String) {
        // Создаем интент для перехода на главный экран приложения
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Настраиваем уведомление
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_SYSTEM_ID)
            .setSmallIcon(R.drawable.ic_notification_system)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        // Показываем уведомление
        showNotification(Constants.NOTIFICATION_SYSTEM_ID, notification)
    }

    /**
     * Показывает уведомление с указанным ID
     *
     * @param notificationId ID уведомления
     * @param notification объект уведомления
     */
    private fun showNotification(notificationId: Int, notification: Notification) {
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Обрабатываем случай, когда у приложения нет разрешения на показ уведомлений
            e.printStackTrace()
        }
    }

    /**
     * Удаляет все уведомления приложения
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * Удаляет уведомление с указанным ID
     *
     * @param notificationId ID уведомления
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}
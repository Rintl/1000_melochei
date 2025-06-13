package com.example.a1000_melochei.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.model.Order
import com.example.a1000_melochei.data.model.OrderStatus
import com.example.a1000_melochei.ui.MainActivity
import com.example.a1000_melochei.util.Constants

/**
 * Сервис для управления уведомлениями приложения.
 * Обеспечивает создание и отправку различных типов уведомлений.
 */
class NotificationService(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    /**
     * Создает уведомление о новом заказе
     */
    fun showNewOrderNotification(order: Order) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("order_id", order.id)
            putExtra("open_orders", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ORDER_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Новый заказ")
            .setContentText("Поступил новый заказ №${order.orderNumber}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Поступил новый заказ №${order.orderNumber}\nСумма: ${order.getFormattedTotal()}\nКлиент: ${order.customerInfo.name}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(Constants.NOTIFICATION_NEW_ORDER_ID, notification)
    }

    /**
     * Создает уведомление об изменении статуса заказа
     */
    fun showOrderStatusNotification(order: Order, newStatus: OrderStatus) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("order_id", order.id)
            putExtra("open_order_detail", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (newStatus) {
            OrderStatus.CONFIRMED -> "подтвержден"
            OrderStatus.PROCESSING -> "обрабатывается"
            OrderStatus.READY_FOR_DELIVERY -> "готов к доставке"
            OrderStatus.IN_DELIVERY -> "доставляется"
            OrderStatus.DELIVERED -> "доставлен"
            OrderStatus.COMPLETED -> "выполнен"
            OrderStatus.CANCELLED -> "отменен"
            else -> "обновлен"
        }

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ORDER_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Статус заказа изменен")
            .setContentText("Заказ №${order.orderNumber} $statusText")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Constants.NOTIFICATION_ORDER_STATUS_ID, notification)
    }

    /**
     * Создает уведомление о низком остатке товара
     */
    fun showLowStockNotification(productName: String, quantity: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_admin", true)
            putExtra("open_products", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_SYSTEM_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Низкий остаток товара")
            .setContentText("$productName: осталось $quantity шт.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Constants.NOTIFICATION_SYSTEM_ID, notification)
    }

    /**
     * Создает уведомление об отсутствии товара на складе
     */
    fun showOutOfStockNotification(productName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_admin", true)
            putExtra("open_products", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_SYSTEM_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Товар закончился")
            .setContentText("$productName отсутствует на складе")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Constants.NOTIFICATION_SYSTEM_ID + 1, notification)
    }

    /**
     * Создает уведомление о промо-акции
     */
    fun showPromotionNotification(title: String, description: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_promotions", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_PROMO_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(description))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Constants.NOTIFICATION_PROMO_ID, notification)
    }

    /**
     * Создает уведомление о напоминании корзины
     */
    fun showCartReminderNotification(itemsCount: Int, totalAmount: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_cart", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_PROMO_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Не забудьте о корзине")
            .setContentText("У вас $itemsCount товаров на сумму ${totalAmount.toInt()} ₸")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Constants.NOTIFICATION_PROMO_ID + 1, notification)
    }

    /**
     * Создает уведомление о доставке
     */
    fun showDeliveryNotification(order: Order, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("order_id", order.id)
            putExtra("open_order_detail", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ORDER_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Доставка заказа №${order.orderNumber}")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(Constants.NOTIFICATION_ORDER_STATUS_ID + 1, notification)
    }

    /**
     * Создает уведомление о системном сообщении
     */
    fun showSystemNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_SYSTEM_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Constants.NOTIFICATION_SYSTEM_ID + 2, notification)
    }

    /**
     * Создает уведомление о групповом обновлении
     */
    fun showGroupNotification(
        groupKey: String,
        notifications: List<NotificationCompat.Builder>,
        summaryTitle: String,
        summaryText: String
    ) {
        // Отправляем индивидуальные уведомления
        notifications.forEachIndexed { index, builder ->
            builder.setGroup(groupKey)
            notificationManager.notify(index + 1000, builder.build())
        }

        // Создаем сводное уведомление
        val summaryNotification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_SYSTEM_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(summaryTitle)
            .setContentText(summaryText)
            .setStyle(NotificationCompat.InboxStyle())
            .setGroup(groupKey)
            .setGroupSummary(true)
            .build()

        notificationManager.notify(0, summaryNotification)
    }

    /**
     * Создает уведомление с прогрессом
     */
    fun showProgressNotification(title: String, text: String, progress: Int, maxProgress: Int): Int {
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_SYSTEM_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(maxProgress, progress, false)
            .setOngoing(true)
            .build()

        notificationManager.notify(notificationId, notification)
        return notificationId
    }

    /**
     * Обновляет уведомление с прогрессом
     */
    fun updateProgressNotification(notificationId: Int, title: String, text: String, progress: Int, maxProgress: Int) {
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_SYSTEM_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(maxProgress, progress, false)
            .setOngoing(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Завершает уведомление с прогрессом
     */
    fun completeProgressNotification(notificationId: Int, title: String, text: String) {
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_SYSTEM_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Отменяет уведомление по ID
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Отменяет все уведомления
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * Проверяет, разрешены ли уведомления
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    /**
     * Проверяет, разрешен ли канал уведомлений
     */
    fun isChannelEnabled(channelId: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(channelId)
            channel?.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            true
        }
    }

    /**
     * Создает каналы уведомлений для Android 8.0+
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ORDER_ID,
                    "Заказы",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Уведомления о новых заказах и изменении статуса"
                    enableLights(true)
                    enableVibration(true)
                },
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_PROMO_ID,
                    "Акции и скидки",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Уведомления о специальных предложениях и скидках"
                    enableLights(true)
                    enableVibration(false)
                },
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_SYSTEM_ID,
                    "Системные уведомления",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Системные уведомления и предупреждения"
                    enableLights(false)
                    enableVibration(false)
                }
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(channels)
        }
    }

    /**
     * Планирует уведомление на определенное время
     */
    fun scheduleNotification(
        title: String,
        message: String,
        triggerTime: Long,
        channelId: String = Constants.NOTIFICATION_CHANNEL_SYSTEM_ID
    ) {
        // В реальном приложении здесь должно быть использование AlarmManager
        // Для демо создаем простое уведомление
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
package com.yourstore.app.service

import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yourstore.app.data.repository.UserRepository
import com.yourstore.app.data.source.local.PreferencesManager
import com.yourstore.app.ui.customer.CustomerActivity
import com.yourstore.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.android.ext.android.inject

/**
 * Сервис для обработки push-уведомлений Firebase Cloud Messaging.
 * Обрабатывает входящие уведомления и управляет токеном устройства.
 */
class MelocheiFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCMService"

    // Инжектируем зависимости через Koin
    private val notificationService: NotificationService by inject()
    private val userRepository: UserRepository by inject()
    private val preferencesManager: PreferencesManager by inject()
    private val firebaseAuth: FirebaseAuth by inject()

    /**
     * Вызывается при получении нового токена устройства.
     * Отправляет токен на сервер для хранения.
     *
     * @param token Новый токен устройства
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Получен новый FCM токен: $token")

        // Сохраняем токен локально
        preferencesManager.setFcmToken(token)

        // Отправляем токен на сервер, если пользователь авторизован
        if (firebaseAuth.currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    userRepository.updateFcmToken(token)
                    Log.d(TAG, "FCM токен успешно обновлен на сервере")
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при обновлении FCM токена на сервере: ${e.message}")
                }
            }
        }
    }

    /**
     * Вызывается при получении нового сообщения.
     * Обрабатывает входящие уведомления в зависимости от их типа.
     *
     * @param remoteMessage Полученное сообщение
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Получено сообщение от: ${remoteMessage.from}")

        // Проверяем, содержит ли сообщение данные
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Данные сообщения: ${remoteMessage.data}")

            // Обрабатываем данные сообщения
            handleRemoteMessage(remoteMessage)
        }

        // Проверяем, содержит ли сообщение уведомление
        remoteMessage.notification?.let {
            Log.d(TAG, "Уведомление: ${it.title} - ${it.body}")

            // Показываем простое уведомление, если не обработали через данные
            showNotification(it.title, it.body)
        }
    }

    /**
     * Обрабатывает данные входящего сообщения и выполняет соответствующие действия.
     *
     * @param remoteMessage Полученное сообщение
     */
    private fun handleRemoteMessage(remoteMessage: RemoteMessage) {
        try {
            val data = remoteMessage.data

            // Получаем тип уведомления
            val type = data["type"] ?: "default"

            when (type) {
                // Уведомление о новом заказе (для администраторов)
                "new_order" -> {
                    val orderId = data["order_id"] ?: ""
                    val orderNumber = data["order_number"] ?: ""
                    val customerName = data["customer_name"] ?: ""

                    if (preferencesManager.isAdmin()) {
                        notificationService.showNewOrderNotification(orderId, orderNumber, customerName)
                    }
                }

                // Уведомление об обновлении статуса заказа (для клиентов)
                "order_status" -> {
                    val orderId = data["order_id"] ?: ""
                    val orderNumber = data["order_number"] ?: ""
                    val newStatus = data["new_status"] ?: ""

                    // Показываем уведомление только клиентам
                    if (!preferencesManager.isAdmin()) {
                        notificationService.showOrderStatusUpdateNotification(orderId, orderNumber, newStatus)
                    }
                }

                // Уведомление о промо-акции
                "promo" -> {
                    val title = data["title"] ?: getString(R.string.notification_promo_default_title)
                    val message = data["message"] ?: ""

                    notificationService.showPromoNotification(title, message)
                }

                // Системное уведомление
                "system" -> {
                    val title = data["title"] ?: getString(R.string.notification_system_default_title)
                    val message = data["message"] ?: ""

                    notificationService.showSystemNotification(title, message)
                }

                // Уведомление с дополнительными действиями
                "action" -> {
                    val action = data["action"] ?: ""
                    handleAction(action, data)
                }

                // По умолчанию, просто показываем уведомление
                else -> {
                    val title = remoteMessage.notification?.title ?: getString(R.string.app_name)
                    val message = remoteMessage.notification?.body ?: ""

                    showNotification(title, message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обработке сообщения: ${e.message}")
        }
    }

    /**
     * Обрабатывает действия, указанные в уведомлении.
     *
     * @param action Тип действия
     * @param data Данные сообщения
     */
    private fun handleAction(action: String, data: Map<String, String>) {
        when (action) {
            // Открыть определенный экран
            "open_screen" -> {
                val screen = data["screen"] ?: ""
                val extraData = data["extra_data"] ?: ""

                // Запускаем соответствующий экран
                when (screen) {
                    "product_detail" -> {
                        openProductDetail(extraData)
                    }
                    "category" -> {
                        openCategory(extraData)
                    }
                    "order_detail" -> {
                        openOrderDetail(extraData)
                    }
                    else -> {
                        // По умолчанию открываем главный экран
                        openMainScreen()
                    }
                }
            }

            // Обновление данных приложения
            "update_data" -> {
                val dataType = data["data_type"] ?: ""

                // Отправляем broadcast для обновления данных в приложении
                val intent = Intent(Constants.ACTION_UPDATE_DATA).apply {
                    putExtra("data_type", dataType)
                }
                sendBroadcast(intent)
            }

            // По умолчанию открываем главный экран
            else -> {
                openMainScreen()
            }
        }
    }

    /**
     * Показывает базовое уведомление с заголовком и сообщением.
     *
     * @param title Заголовок уведомления
     * @param message Текст уведомления
     */
    private fun showNotification(title: String?, message: String?) {
        if (!title.isNullOrEmpty() && !message.isNullOrEmpty()) {
            notificationService.showSystemNotification(title, message)
        }
    }

    /**
     * Открывает главный экран приложения.
     */
    private fun openMainScreen() {
        val intent = Intent(this, CustomerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    /**
     * Открывает экран деталей товара.
     *
     * @param productId ID товара
     */
    private fun openProductDetail(productId: String) {
        val intent = Intent(this, com.yourstore.app.ui.customer.catalog.ProductDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("PRODUCT_ID", productId)
        }
        startActivity(intent)
    }

    /**
     * Открывает экран категории.
     *
     * @param categoryId ID категории
     */
    private fun openCategory(categoryId: String) {
        val intent = Intent(this, CustomerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_CATEGORY", true)
            putExtra("CATEGORY_ID", categoryId)
        }
        startActivity(intent)
    }

    /**
     * Открывает экран деталей заказа.
     *
     * @param orderId ID заказа
     */
    private fun openOrderDetail(orderId: String) {
        val intent = Intent(this, com.yourstore.app.ui.customer.orders.OrderDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ORDER_ID", orderId)
        }
        startActivity(intent)
    }
}
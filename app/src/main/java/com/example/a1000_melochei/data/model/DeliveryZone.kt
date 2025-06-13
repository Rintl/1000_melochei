package com.example.a1000_melochei.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных для зоны доставки.
 * Используется для определения стоимости и условий доставки в зависимости от местоположения.
 */
@Parcelize
data class DeliveryZone(
    val id: String = "", // Уникальный идентификатор зоны доставки
    val name: String = "", // Название зоны доставки (например, "Центр", "Окраина", "Пригород")
    val description: String = "", // Описание зоны

    // Условия доставки
    val deliveryFee: Double = 0.0, // Базовая стоимость доставки
    val freeDeliveryThreshold: Double? = null, // Порог для бесплатной доставки (null - бесплатная доставка недоступна)
    val minOrderAmount: Double = 0.0, // Минимальная сумма заказа для доставки
    val estimatedDeliveryTime: IntRange = 60..120, // Ориентировочное время доставки в минутах

    // Расписание доставки
    val deliveryDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // Дни доставки (1 - понедельник, 7 - воскресенье)
    val deliveryHoursStart: Int = 9, // Час начала доставки (24-часовой формат)
    val deliveryHoursEnd: Int = 18, // Час окончания доставки (24-часовой формат)

    // Географические границы зоны
    val boundaries: List<GeoPoint> = emptyList(), // Границы зоны в виде полигона
    val center: GeoPoint? = null, // Центр зоны
    val radius: Double? = null, // Радиус зоны в км (для круговых зон)

    // Дополнительные настройки
    val isActive: Boolean = true, // Активна ли зона доставки
    val priority: Int = 0, // Приоритет зоны (используется при пересечении зон)
    val additionalFees: Map<String, Double> = mapOf() // Дополнительные сборы (например, "тяжелый груз", "хрупкий товар")
) : Parcelable {

    /**
     * Вычисляет стоимость доставки в зависимости от суммы заказа
     */
    fun calculateDeliveryFee(orderAmount: Double): Double {
        // Если заказ превышает порог бесплатной доставки и порог установлен
        if (freeDeliveryThreshold != null && orderAmount >= freeDeliveryThreshold) {
            return 0.0
        }
        return deliveryFee
    }

    /**
     * Проверяет, доступна ли доставка в указанный день недели
     * @param dayOfWeek день недели (1 - понедельник, 7 - воскресенье)
     */
    fun isDeliveryAvailable(dayOfWeek: Int): Boolean {
        return deliveryDays.contains(dayOfWeek)
    }

    /**
     * Проверяет, доступна ли доставка в указанное время
     * @param hour час (24-часовой формат)
     */
    fun isDeliveryTimeAvailable(hour: Int): Boolean {
        return hour in deliveryHoursStart..deliveryHoursEnd
    }

    /**
     * Форматирует рабочие часы доставки в читаемый формат
     */
    fun getFormattedDeliveryHours(): String {
        return "с $deliveryHoursStart:00 до $deliveryHoursEnd:00"
    }

    /**
     * Форматирует ориентировочное время доставки в читаемый формат
     */
    fun getFormattedEstimatedTime(): String {
        return if (estimatedDeliveryTime.first == estimatedDeliveryTime.last) {
            "примерно ${estimatedDeliveryTime.first} мин."
        } else {
            "от ${estimatedDeliveryTime.first} до ${estimatedDeliveryTime.last} мин."
        }
    }
}
package com.example.a1000_melochei.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Модель данных для акций и специальных предложений.
 * Используется для хранения информации о скидках, промо-кодах и других маркетинговых акциях.
 */
@Parcelize
data class Promotion(
    val id: String = "", // Уникальный идентификатор акции
    val title: String = "", // Заголовок акции
    val description: String = "", // Описание акции
    val imageUrl: String = "", // URL изображения для акции

    // Период действия акции
    val startDate: Long = 0, // Дата начала акции (timestamp)
    val endDate: Long? = null, // Дата окончания акции (timestamp) (null - бессрочная акция)

    // Типы акций
    val type: String = "", // Тип акции: "discount", "promo_code", "gift", "bundle", "category", "product"
    val discountType: String = "percent", // Тип скидки: "percent", "fixed", "free_shipping"
    val discountValue: Double = 0.0, // Размер скидки (процент или фиксированная сумма в зависимости от типа)

    // Применимость акции
    val categoryId: String? = null, // Идентификатор категории, если акция применяется к категории
    val productId: String? = null, // Идентификатор товара, если акция применяется к конкретному товару
    val applicableProductIds: List<String> = emptyList(), // Список ID товаров, к которым применима акция
    val excludedProductIds: List<String> = emptyList(), // Список ID товаров, исключенных из акции

    // Промо-код
    val promoCode: String? = null, // Промо-код (если применимо)
    val maxUsesTotal: Int? = null, // Максимальное общее количество использований
    val maxUsesPerUser: Int? = null, // Максимальное количество использований на одного пользователя
    val currentUses: Int = 0, // Текущее количество использований

    // Условия применения
    val minOrderAmount: Double? = null, // Минимальная сумма заказа для применения акции
    val minQuantity: Int? = null, // Минимальное количество товаров

    // Дополнительные параметры
    val isActive: Boolean = true, // Активна ли акция
    val priority: Int = 0, // Приоритет акции (для определения порядка применения)
    val tags: List<String> = emptyList(), // Теги для фильтрации и поиска

    // Специальный параметр для отображения процента скидки
    val discountPercent: Int = 0 // Рассчитанный процент скидки для отображения
) : Parcelable {

    /**
     * Проверяет, активна ли акция в данный момент времени
     */
    fun isCurrentlyActive(): Boolean {
        val now = System.currentTimeMillis()
        val isInTimeRange = now >= startDate && (endDate == null || now <= endDate)
        return isActive && isInTimeRange
    }

    /**
     * Вычисляет размер скидки для указанной цены
     */
    fun calculateDiscount(price: Double): Double {
        return when (discountType) {
            "percent" -> price * discountValue / 100
            "fixed" -> discountValue
            else -> 0.0
        }
    }

    /**
     * Вычисляет цену со скидкой
     */
    fun calculateDiscountedPrice(price: Double): Double {
        val discount = calculateDiscount(price)
        return (price - discount).coerceAtLeast(0.0) // Цена не может быть отрицательной
    }

    /**
     * Проверяет, применима ли акция к указанному товару
     */
    fun isApplicableToProduct(product: Product): Boolean {
        // Если акция не активна, то не применима
        if (!isCurrentlyActive()) return false

        // Проверяем, исключен ли товар явно
        if (excludedProductIds.contains(product.id)) return false

        // Проверяем применимость по ID товара или категории
        return when {
            // Если акция для конкретного товара
            productId != null -> product.id == productId

            // Если акция для категории
            categoryId != null -> product.categoryId == categoryId

            // Если акция для списка товаров
            applicableProductIds.isNotEmpty() -> applicableProductIds.contains(product.id)

            // По умолчанию акция применима ко всем товарам
            else -> true
        }
    }

    /**
     * Получает оставшееся время действия акции в днях
     */
    fun getRemainingDays(): Int? {
        val now = System.currentTimeMillis()
        return endDate?.let {
            val diff = it - now
            (diff / (24 * 60 * 60 * 1000)).toInt()
        }
    }

    /**
     * Возвращает форматированный текст с условиями акции
     */
    fun getFormattedConditions(): String {
        val conditions = mutableListOf<String>()

        minOrderAmount?.let { conditions.add("Минимальная сумма заказа: $it") }
        minQuantity?.let { conditions.add("Минимальное количество товаров: $it") }
        endDate?.let {
            val days = getRemainingDays()
            if (days != null && days > 0) {
                conditions.add("Осталось дней: $days")
            }
        }

        return conditions.joinToString("\n")
    }
}
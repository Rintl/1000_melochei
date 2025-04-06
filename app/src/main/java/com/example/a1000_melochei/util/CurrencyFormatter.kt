package com.yourstore.app.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Утилитный класс для форматирования валютных значений.
 * Используется для отображения цен и других денежных сумм в правильном формате.
 */
object CurrencyFormatter {

    // Символ валюты - тенге
    private const val CURRENCY_SYMBOL = "₸"

    // Локаль для Казахстана
    private val KZ_LOCALE = Locale("ru", "KZ")

    /**
     * Форматирует число как валюту с символом тенге.
     *
     * @param amount Сумма для форматирования
     * @param showSymbolFirst Показывать ли символ валюты перед числом
     * @param showDecimals Показывать ли десятичные знаки
     * @return Отформатированная строка с валютой
     */
    fun format(amount: Double, showSymbolFirst: Boolean = false, showDecimals: Boolean = true): String {
        val formatter = NumberFormat.getNumberInstance(KZ_LOCALE) as DecimalFormat

        // Настраиваем отображение десятичных знаков
        if (showDecimals) {
            formatter.minimumFractionDigits = 2
            formatter.maximumFractionDigits = 2
        } else {
            formatter.minimumFractionDigits = 0
            formatter.maximumFractionDigits = 0
        }

        // Формируем строку с учетом положения символа валюты
        return if (showSymbolFirst) {
            "$CURRENCY_SYMBOL ${formatter.format(amount)}"
        } else {
            "${formatter.format(amount)} $CURRENCY_SYMBOL"
        }
    }

    /**
     * Форматирует число как валюту, автоматически определяя необходимость отображения
     * десятичных знаков (показывает их, только если сумма не целая).
     *
     * @param amount Сумма для форматирования
     * @param showSymbolFirst Показывать ли символ валюты перед числом
     * @return Отформатированная строка с валютой
     */
    fun formatAuto(amount: Double, showSymbolFirst: Boolean = false): String {
        val isWholeNumber = amount == amount.toLong().toDouble()
        return format(amount, showSymbolFirst, !isWholeNumber)
    }

    /**
     * Форматирует цену со скидкой, показывая старую цену зачеркнутой.
     *
     * @param originalPrice Оригинальная цена
     * @param discountPrice Цена со скидкой
     * @return HTML-строка с отформатированными ценами
     */
    fun formatWithDiscount(originalPrice: Double, discountPrice: Double): String {
        val formattedOriginal = format(originalPrice, false, false)
        val formattedDiscount = format(discountPrice, false, false)

        // Вычисляем процент скидки
        val discountPercent = ((originalPrice - discountPrice) / originalPrice * 100).toInt()

        return "<s>$formattedOriginal</s> $formattedDiscount (-$discountPercent%)"
    }

    /**
     * Форматирует строку с разделением на разряды.
     * Например: 1000000 -> "1 000 000"
     *
     * @param amount Сумма для форматирования
     * @return Отформатированная строка с разделением разрядов
     */
    fun formatWithSpaces(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(KZ_LOCALE) as DecimalFormat
        formatter.isGroupingUsed = true
        formatter.groupingSize = 3

        return formatter.format(amount)
    }

    /**
     * Парсит строку, содержащую денежную сумму, в число.
     *
     * @param amountStr Строка с денежной суммой
     * @return Число или null в случае ошибки парсинга
     */
    fun parse(amountStr: String): Double? {
        val cleanStr = amountStr
            .replace(CURRENCY_SYMBOL, "")
            .replace("\\s".toRegex(), "")
            .trim()

        return try {
            val formatter = NumberFormat.getNumberInstance(KZ_LOCALE) as DecimalFormat
            formatter.parse(cleanStr)?.toDouble()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Рассчитывает процент скидки между двумя ценами.
     *
     * @param originalPrice Исходная цена
     * @param discountPrice Цена со скидкой
     * @return Процент скидки (целое число)
     */
    fun calculateDiscountPercent(originalPrice: Double, discountPrice: Double): Int {
        if (originalPrice <= 0 || discountPrice >= originalPrice) return 0
        return ((originalPrice - discountPrice) / originalPrice * 100).toInt()
    }
}
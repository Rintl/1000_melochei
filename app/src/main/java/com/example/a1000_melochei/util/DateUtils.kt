package com.example.a1000_melochei.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit


/**
 * Класс-утилита для работы с датами и временем в приложении.
 * Предоставляет методы для форматирования, парсинга и сравнения дат.
 */
object DateUtils {

    private val DEFAULT_LOCALE = Locale("ru", "KZ")

    /**
     * Форматирует дату в стандартный формат день.месяц.год
     *
     * @param date дата для форматирования
     * @return строка с отформатированной датой
     */
    fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat(Constants.DEFAULT_DATE_FORMAT, DEFAULT_LOCALE)
        return formatter.format(date)
    }

    /**
     * Форматирует время в стандартный формат часы:минуты
     *
     * @param date дата для форматирования
     * @return строка с отформатированным временем
     */
    fun formatTime(date: Date): String {
        val formatter = SimpleDateFormat(Constants.DEFAULT_TIME_FORMAT, DEFAULT_LOCALE)
        return formatter.format(date)
    }

    /**
     * Форматирует дату и время в стандартный формат день.месяц.год часы:минуты
     *
     * @param date дата для форматирования
     * @return строка с отформатированной датой и временем
     */
    fun formatDateTime(date: Date): String {
        val formatter = SimpleDateFormat(Constants.DEFAULT_DATETIME_FORMAT, DEFAULT_LOCALE)
        return formatter.format(date)
    }

    /**
     * Форматирует timestamp (миллисекунды с 1970 года) в стандартный формат
     *
     * @param timestamp временная метка в миллисекундах
     * @return строка с отформатированной датой и временем
     */
    fun formatDateTime(timestamp: Long): String {
        return formatDateTime(Date(timestamp))
    }

    /**
     * Парсит строку с датой в объект Date
     *
     * @param dateString строка с датой в формате день.месяц.год
     * @return объект Date или null, если парсинг не удался
     */
    fun parseDate(dateString: String): Date? {
        return try {
            val formatter = SimpleDateFormat(Constants.DEFAULT_DATE_FORMAT, DEFAULT_LOCALE)
            formatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Парсит строку с датой и временем в объект Date
     *
     * @param dateTimeString строка с датой и временем в формате день.месяц.год часы:минуты
     * @return объект Date или null, если парсинг не удался
     */
    fun parseDateTime(dateTimeString: String): Date? {
        return try {
            val formatter = SimpleDateFormat(Constants.DEFAULT_DATETIME_FORMAT, DEFAULT_LOCALE)
            formatter.parse(dateTimeString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Форматирует дату в формат для отображения в зависимости от того, насколько она недавняя
     * Например: "Сегодня", "Вчера", "27 мая" и т.д.
     *
     * @param date дата для форматирования
     * @return строка с отформатированной датой для отображения
     */
    fun formatDateForDisplay(date: Date): String {
        val now = System.currentTimeMillis()

        return when {
            DateUtils.isToday(date.time) -> "Сегодня"
            DateUtils.isToday(date.time + TimeUnit.DAYS.toMillis(1)) -> "Вчера"
            date.time > now - TimeUnit.DAYS.toMillis(7) -> {
                // В течение недели показываем день недели
                val dayFormat = SimpleDateFormat("EEEE", DEFAULT_LOCALE)
                dayFormat.format(date).capitalizeFirstLetter()
            }
            else -> {
                // Иначе показываем дату в формате день месяц
                val formatter = SimpleDateFormat("d MMMM", DEFAULT_LOCALE)
                formatter.format(date)
            }
        }
    }

    /**
     * Форматирует время для отображения в зависимости от того, насколько оно недавнее
     * Например: "Только что", "5 минут назад", "2 часа назад" и т.д.
     *
     * @param timestamp временная метка в миллисекундах
     * @return строка с отформатированным временем для отображения
     */
    fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Только что"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff).toInt()
                "$minutes ${pluralizeMinutes(minutes)} назад"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff).toInt()
                "$hours ${pluralizeHours(hours)} назад"
            }
            diff < TimeUnit.DAYS.toMillis(2) -> "Вчера"
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
                "$days ${pluralizeDays(days)} назад"
            }
            else -> formatDate(Date(timestamp))
        }
    }

    /**
     * Возвращает корректное склонение для слова "минута" в зависимости от числа
     *
     * @param count количество минут
     * @return правильное склонение слова "минута"
     */
    private fun pluralizeMinutes(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "минуту"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "минуты"
            else -> "минут"
        }
    }

    /**
     * Возвращает корректное склонение для слова "час" в зависимости от числа
     *
     * @param count количество часов
     * @return правильное склонение слова "час"
     */
    private fun pluralizeHours(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "час"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "часа"
            else -> "часов"
        }
    }

    /**
     * Возвращает корректное склонение для слова "день" в зависимости от числа
     *
     * @param count количество дней
     * @return правильное склонение слова "день"
     */
    private fun pluralizeDays(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "день"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "дня"
            else -> "дней"
        }
    }

    /**
     * Создает объект Date из компонентов (год, месяц, день, часы, минуты)
     *
     * @param year год
     * @param month месяц (0-11)
     * @param day день месяца
     * @param hour часы
     * @param minute минуты
     * @return объект Date
     */
    fun createDate(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hour, minute, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    /**
     * Возвращает начало дня для указанной даты
     *
     * @param date исходная дата
     * @return дата, соответствующая началу дня (00:00:00)
     */
    fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    /**
     * Возвращает конец дня для указанной даты
     *
     * @param date исходная дата
     * @return дата, соответствующая концу дня (23:59:59.999)
     */
    fun getEndOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    /**
     * Возвращает начало месяца для указанной даты
     *
     * @param date исходная дата
     * @return дата, соответствующая первому дню месяца (00:00:00)
     */
    fun getStartOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return getStartOfDay(calendar.time)
    }

    /**
     * Возвращает конец месяца для указанной даты
     *
     * @param date исходная дата
     * @return дата, соответствующая последнему дню месяца (23:59:59.999)
     */
    fun getEndOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        return getEndOfDay(calendar.time)
    }

    /**
     * Добавляет указанное количество дней к дате
     *
     * @param date исходная дата
     * @param days количество дней для добавления (может быть отрицательным)
     * @return новая дата после добавления дней
     */
    fun addDays(date: Date, days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.time
    }

    /**
     * Добавляет указанное количество часов к дате
     *
     * @param date исходная дата
     * @param hours количество часов для добавления (может быть отрицательным)
     * @return новая дата после добавления часов
     */
    fun addHours(date: Date, hours: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.HOUR_OF_DAY, hours)
        return calendar.time
    }

    /**
     * Проверяет, находится ли дата в прошлом относительно текущего момента
     *
     * @param date проверяемая дата
     * @return true, если дата в прошлом
     */
    fun isPast(date: Date): Boolean {
        return date.before(Date())
    }

    /**
     * Проверяет, находится ли дата в будущем относительно текущего момента
     *
     * @param date проверяемая дата
     * @return true, если дата в будущем
     */
    fun isFuture(date: Date): Boolean {
        return date.after(Date())
    }

    /**
     * Вычисляет разницу в днях между двумя датами
     *
     * @param date1 первая дата
     * @param date2 вторая дата
     * @return разница в днях (положительное число, если date1 раньше date2)
     */
    fun daysBetween(date1: Date, date2: Date): Int {
        val diff = date2.time - date1.time
        return TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }

    /**
     * Конвертирует дату из одного часового пояса в другой
     *
     * @param date исходная дата
     * @param fromTimeZone исходный часовой пояс (например, "Europe/Moscow")
     * @param toTimeZone целевой часовой пояс (например, "Asia/Almaty")
     * @return дата в целевом часовом поясе
     */
    fun convertTimeZone(date: Date, fromTimeZone: String, toTimeZone: String): Date {
        val sourceCalendar = Calendar.getInstance()
        sourceCalendar.time = date
        sourceCalendar.timeZone = TimeZone.getTimeZone(fromTimeZone)

        val destCalendar = Calendar.getInstance()
        destCalendar.timeZone = TimeZone.getTimeZone(toTimeZone)

        destCalendar.set(
            sourceCalendar.get(Calendar.YEAR),
            sourceCalendar.get(Calendar.MONTH),
            sourceCalendar.get(Calendar.DAY_OF_MONTH),
            sourceCalendar.get(Calendar.HOUR_OF_DAY),
            sourceCalendar.get(Calendar.MINUTE),
            sourceCalendar.get(Calendar.SECOND)
        )

        return destCalendar.time
    }

    /**
     * Возвращает текущую дату и время как строку в стандартном формате
     *
     * @return текущая дата и время в формате dd.MM.yyyy HH:mm
     */
    fun getCurrentDateTimeString(): String {
        return formatDateTime(Date())
    }

    /**
     * Форматирует строку даты из одного формата в другой
     *
     * @param dateString исходная строка с датой
     * @param fromFormat исходный формат
     * @param toFormat целевой формат
     * @return строка с датой в целевом формате или null, если преобразование не удалось
     */
    fun reformatDate(dateString: String, fromFormat: String, toFormat: String): String? {
        return try {
            val fromFormatter = SimpleDateFormat(fromFormat, DEFAULT_LOCALE)
            val date = fromFormatter.parse(dateString) ?: return null

            val toFormatter = SimpleDateFormat(toFormat, DEFAULT_LOCALE)
            toFormatter.format(date)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Преобразует серверную дату в локальную
     *
     * @param serverDateString строка с датой в серверном формате
     * @return объект Date в локальном часовом поясе
     */
    fun parseServerDate(serverDateString: String): Date? {
        return try {
            val formatter = SimpleDateFormat(Constants.SERVER_DATETIME_FORMAT, Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            formatter.parse(serverDateString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Переводит первую букву строки в верхний регистр
     *
     * @return строка с первой заглавной буквой
     */
    private fun String.capitalizeFirstLetter(): String {
        if (isEmpty()) return this
        return this[0].uppercaseChar() + substring(1)
    }
}
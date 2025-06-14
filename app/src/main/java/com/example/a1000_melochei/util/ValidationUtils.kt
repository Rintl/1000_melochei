package com.example.a1000_melochei.util

import android.text.TextUtils
import android.util.Patterns
import java.util.regex.Pattern


/**
 * Класс-утилита для валидации пользовательского ввода в формах приложения.
 * Предоставляет методы для проверки корректности email, пароля, телефона и других данных.
 */
object ValidationUtils {

    /**
     * Проверяет корректность email адреса
     *
     * @param email адрес электронной почты для проверки
     * @return true, если email является валидным
     */
    fun isValidEmail(email: String): Boolean {
        if (TextUtils.isEmpty(email)) return false

        // Проверка через регулярное выражение из Constants
        val pattern = Pattern.compile(Constants.EMAIL_REGEX)
        val matcher = pattern.matcher(email)

        return matcher.matches()
    }

    /**
     * Проверяет корректность пароля
     * Минимальная длина: 6 символов
     * Максимальная длина: 20 символов
     *
     * @param password пароль для проверки
     * @return true, если пароль соответствует требованиям
     */
    fun isValidPassword(password: String): Boolean {
        // Проверка длины
        if (password.length < Constants.MIN_PASSWORD_LENGTH || password.length > Constants.MAX_PASSWORD_LENGTH) {
            return false
        }

        // Опционально можно добавить дополнительные проверки на сложность пароля
        // Например, наличие цифр, специальных символов, заглавных букв и т.д.

        return true
    }

    /**
     * Проверяет сложный пароль
     * Минимальная длина: 8 символов
     * Должен содержать хотя бы одну цифру
     * Должен содержать хотя бы одну заглавную букву
     * Должен содержать хотя бы один специальный символ
     *
     * @param password пароль для проверки
     * @return true, если пароль соответствует всем требованиям
     */
    fun isStrongPassword(password: String): Boolean {
        // Проверка длины
        if (password.length < 8) {
            return false
        }

        // Проверка наличия хотя бы одной цифры
        if (!password.any { it.isDigit() }) {
            return false
        }

        // Проверка наличия хотя бы одной заглавной буквы
        if (!password.any { it.isUpperCase() }) {
            return false
        }

        // Проверка наличия хотя бы одного специального символа
        val specialChars = "!@#$%^&*()_-+=<>?/[]{}"
        if (!password.any { specialChars.contains(it) }) {
            return false
        }

        return true
    }

    /**
     * Проверяет корректность номера телефона
     *
     * @param phone номер телефона для проверки
     * @return true, если номер телефона является валидным
     */
    fun isValidPhone(phone: String): Boolean {
        if (TextUtils.isEmpty(phone)) return false

        // Удаляем все нецифровые символы для нормализации номера
        val digitsOnly = phone.replace(Regex("[^\\d+]"), "")

        // Проверка через регулярное выражение из Constants
        val pattern = Pattern.compile(Constants.PHONE_REGEX)
        val matcher = pattern.matcher(digitsOnly)

        return matcher.matches()
    }

    /**
     * Проверяет корректность имени пользователя
     *
     * @param name имя для проверки
     * @return true, если имя соответствует требованиям
     */
    fun isValidName(name: String): Boolean {
        if (TextUtils.isEmpty(name)) return false

        // Проверка на минимальную длину
        if (name.length < Constants.MIN_NAME_LENGTH) {
            return false
        }

        // Проверка, что имя содержит только буквы и пробелы
        // Разрешаем буквы любого алфавита, пробелы и дефисы
        val pattern = Pattern.compile("^[\\p{L} \\-]+$")
        val matcher = pattern.matcher(name)

        return matcher.matches()
    }

    /**
     * Проверяет корректность адреса
     *
     * @param address адрес для проверки
     * @return true, если адрес соответствует требованиям
     */
    fun isValidAddress(address: String): Boolean {
        if (TextUtils.isEmpty(address)) return false

        // Проверка на минимальную длину
        return address.length >= Constants.MIN_ADDRESS_LENGTH
    }

    /**
     * Проверяет корректность индекса (почтового кода)
     *
     * @param postalCode индекс для проверки
     * @return true, если индекс является валидным
     */
    fun isValidPostalCode(postalCode: String): Boolean {
        if (TextUtils.isEmpty(postalCode)) return false

        // Проверка на формат казахстанского почтового индекса (6 цифр)
        val pattern = Pattern.compile("^\\d{6}$")
        val matcher = pattern.matcher(postalCode)

        return matcher.matches()
    }

    /**
     * Проверяет корректность ИИН (Индивидуальный идентификационный номер)
     *
     * @param iin ИИН для проверки
     * @return true, если ИИН является валидным
     */
    fun isValidIIN(iin: String): Boolean {
        if (TextUtils.isEmpty(iin)) return false

        // Проверка на формат казахстанского ИИН (12 цифр)
        val pattern = Pattern.compile("^\\d{12}$")
        val matcher = pattern.matcher(iin)

        // Базовая проверка формата
        if (!matcher.matches()) {
            return false
        }

        // Можно добавить более сложную проверку ИИН с учетом алгоритма формирования
        // и контрольной цифры, но это уже будет специфично для конкретной страны

        return true
    }

    /**
     * Проверяет корректность БИН (Бизнес-идентификационный номер)
     *
     * @param bin БИН для проверки
     * @return true, если БИН является валидным
     */
    fun isValidBIN(bin: String): Boolean {
        if (TextUtils.isEmpty(bin)) return false

        // Проверка на формат казахстанского БИН (12 цифр)
        val pattern = Pattern.compile("^\\d{12}$")
        val matcher = pattern.matcher(bin)

        return matcher.matches()
    }

    /**
     * Проверяет корректность URL
     *
     * @param url URL для проверки
     * @return true, если URL является валидным
     */
    fun isValidUrl(url: String): Boolean {
        if (TextUtils.isEmpty(url)) return false

        return Patterns.WEB_URL.matcher(url).matches()
    }

    /**
     * Проверяет корректность числа (целого или с плавающей точкой)
     *
     * @param number строковое представление числа
     * @return true, если строка представляет корректное число
     */
    fun isValidNumber(number: String): Boolean {
        if (TextUtils.isEmpty(number)) return false

        return try {
            number.toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * Проверяет, является ли строка положительным целым числом
     *
     * @param number строковое представление числа
     * @return true, если строка представляет положительное целое число
     */
    fun isPositiveInteger(number: String): Boolean {
        if (TextUtils.isEmpty(number)) return false

        return try {
            val n = number.toInt()
            n > 0
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * Проверяет, является ли строка положительным числом с плавающей точкой
     *
     * @param number строковое представление числа
     * @return true, если строка представляет положительное число
     */
    fun isPositiveNumber(number: String): Boolean {
        if (TextUtils.isEmpty(number)) return false

        return try {
            val n = number.toDouble()
            n > 0
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * Проверяет, находится ли число в заданном диапазоне
     *
     * @param number строковое представление числа
     * @param min минимальное значение (включительно)
     * @param max максимальное значение (включительно)
     * @return true, если число находится в диапазоне [min, max]
     */
    fun isNumberInRange(number: String, min: Double, max: Double): Boolean {
        if (!isValidNumber(number)) return false

        val n = number.toDouble()
        return n >= min && n <= max
    }
}
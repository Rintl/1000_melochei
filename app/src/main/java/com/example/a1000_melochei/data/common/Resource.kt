package com.example.a1000_melochei.data.common

/**
 * Универсальный класс-обертка для обработки состояний данных.
 * Используется для представления трех основных состояний: загрузка, успех, ошибка.
 *
 * @param T тип данных, которые обрабатываются
 */
sealed class Resource<out T>(
    val data: T? = null,
    val message: String? = null
) {
    /**
     * Состояние успешной загрузки данных
     * @param data загруженные данные
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * Состояние ошибки при загрузке данных
     * @param message сообщение об ошибке
     * @param data частично загруженные данные (опционально)
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    /**
     * Состояние загрузки данных
     * @param data кэшированные данные для отображения во время загрузки (опционально)
     */
    class Loading<T>(data: T? = null) : Resource<T>(data)

    /**
     * Проверяет, является ли текущее состояние успешным
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Проверяет, является ли текущее состояние ошибкой
     */
    fun isError(): Boolean = this is Error

    /**
     * Проверяет, является ли текущее состояние загрузкой
     */
    fun isLoading(): Boolean = this is Loading

    /**
     * Возвращает данные, если состояние успешное, иначе null
     */
    fun getDataOrNull(): T? = if (this is Success) data else null

    /**
     * Возвращает сообщение об ошибке, если состояние ошибочное, иначе null
     */
    fun getErrorMessage(): String? = if (this is Error) message else null

    /**
     * Выполняет действие при успешном состоянии
     */
    inline fun onSuccess(action: (data: T) -> Unit): Resource<T> {
        if (this is Success && data != null) {
            action(data)
        }
        return this
    }

    /**
     * Выполняет действие при состоянии ошибки
     */
    inline fun onError(action: (message: String) -> Unit): Resource<T> {
        if (this is Error && message != null) {
            action(message)
        }
        return this
    }

    /**
     * Выполняет действие при состоянии загрузки
     */
    inline fun onLoading(action: () -> Unit): Resource<T> {
        if (this is Loading) {
            action()
        }
        return this
    }

    /**
     * Преобразует данные в Resource другого типа
     */
    fun <R> map(transform: (T) -> R): Resource<R> {
        return when (this) {
            is Success -> {
                if (data != null) {
                    try {
                        Success(transform(data))
                    } catch (e: Exception) {
                        Error(e.message ?: "Ошибка преобразования данных")
                    }
                } else {
                    Error("Данные отсутствуют")
                }
            }
            is Error -> Error(message ?: "Неизвестная ошибка", null)
            is Loading -> Loading(null)
        }
    }

    /**
     * Преобразует данные в Resource другого типа с обработкой nullable
     */
    fun <R> mapNullable(transform: (T?) -> R?): Resource<R> {
        return when (this) {
            is Success -> {
                try {
                    val result = transform(data)
                    if (result != null) {
                        Success(result)
                    } else {
                        Error("Результат преобразования равен null")
                    }
                } catch (e: Exception) {
                    Error(e.message ?: "Ошибка преобразования данных")
                }
            }
            is Error -> Error(message ?: "Неизвестная ошибка", null)
            is Loading -> Loading(null)
        }
    }

    /**
     * Возвращает данные или значение по умолчанию
     */
    fun getDataOrDefault(defaultValue: T): T {
        return if (this is Success && data != null) data else defaultValue
    }

    companion object {
        /**
         * Создает Resource.Success с данными
         */
        fun <T> success(data: T): Resource<T> = Success(data)

        /**
         * Создает Resource.Error с сообщением
         */
        fun <T> error(message: String, data: T? = null): Resource<T> = Error(message, data)

        /**
         * Создает Resource.Loading
         */
        fun <T> loading(data: T? = null): Resource<T> = Loading(data)
    }
}

/**
 * Расширение для удобной работы с Resource в coroutines
 */
suspend fun <T> Resource<T>.doOnSuccess(action: suspend (T) -> Unit): Resource<T> {
    if (this is Resource.Success && data != null) {
        action(data)
    }
    return this
}

/**
 * Расширение для удобной работы с Resource в coroutines при ошибке
 */
suspend fun <T> Resource<T>.doOnError(action: suspend (String) -> Unit): Resource<T> {
    if (this is Resource.Error && message != null) {
        action(message)
    }
    return this
}

/**
 * Расширение для удобной работы с Resource в coroutines при загрузке
 */
suspend fun <T> Resource<T>.doOnLoading(action: suspend () -> Unit): Resource<T> {
    if (this is Resource.Loading) {
        action()
    }
    return this
}
package com.example.a1000_melochei.data.common

/**
 * Универсальный класс-обертка для обработки состояний данных.
 * Используется для представления трех основных состояний: загрузка, успех, ошибка.
 *
 * @param T тип данных, которые обрабатываются
 */
sealed class Resource<T>(
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
                try {
                    Success(transform(data))
                } catch (e: Exception) {
                    Error(e.message ?: "Ошибка преобразования данных")
                }
            }
            is Error -> Error(message ?: "Неизвестная ошибка", null)
            is Loading -> Loading(null)
        }
    }

    /**
     * Преобразует данные в Resource другого типа с возможностью ошибки
     */
    inline fun <R> flatMap(transform: (T) -> Resource<R>): Resource<R> {
        return when (this) {
            is Success -> {
                try {
                    transform(data)
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
        return when (this) {
            is Success -> data
            else -> defaultValue
        }
    }

    /**
     * Возвращает данные или выбрасывает исключение
     */
    fun getDataOrThrow(): T {
        return when (this) {
            is Success -> data
            is Error -> throw Exception(message ?: "Неизвестная ошибка")
            is Loading -> throw Exception("Данные еще загружаются")
        }
    }

    override fun toString(): String {
        return when (this) {
            is Success -> "Success[data=$data]"
            is Error -> "Error[message=$message]"
            is Loading -> "Loading[data=$data]"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Resource<*>) return false

        return when {
            this is Success && other is Success -> this.data == other.data
            this is Error && other is Error -> this.message == other.message && this.data == other.data
            this is Loading && other is Loading -> this.data == other.data
            else -> false
        }
    }

    override fun hashCode(): Int {
        return when (this) {
            is Success -> data?.hashCode() ?: 0
            is Error -> (message?.hashCode() ?: 0) * 31 + (data?.hashCode() ?: 0)
            is Loading -> data?.hashCode() ?: 0
        }
    }

    companion object {
        /**
         * Создает успешный Resource
         */
        fun <T> success(data: T): Resource<T> = Success(data)

        /**
         * Создает Resource с ошибкой
         */
        fun <T> error(message: String, data: T? = null): Resource<T> = Error(message, data)

        /**
         * Создает Resource в состоянии загрузки
         */
        fun <T> loading(data: T? = null): Resource<T> = Loading(data)

        /**
         * Создает Resource из nullable значения
         */
        fun <T> fromNullable(data: T?, errorMessage: String = "Данные не найдены"): Resource<T> {
            return if (data != null) {
                Success(data)
            } else {
                Error(errorMessage)
            }
        }

        /**
         * Создает Resource из результата операции
         */
        inline fun <T> fromResult(operation: () -> T): Resource<T> {
            return try {
                Success(operation())
            } catch (e: Exception) {
                Error(e.message ?: "Неизвестная ошибка")
            }
        }

        /**
         * Объединяет два Resource в один
         */
        fun <T, R, S> combine(
            resource1: Resource<T>,
            resource2: Resource<R>,
            combiner: (T, R) -> S
        ): Resource<S> {
            return when {
                resource1 is Success && resource2 is Success -> {
                    try {
                        Success(combiner(resource1.data, resource2.data))
                    } catch (e: Exception) {
                        Error(e.message ?: "Ошибка объединения данных")
                    }
                }
                resource1 is Error -> Error(resource1.message ?: "Ошибка в первом источнике")
                resource2 is Error -> Error(resource2.message ?: "Ошибка во втором источнике")
                else -> Loading()
            }
        }
    }
}
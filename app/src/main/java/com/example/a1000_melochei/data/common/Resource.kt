package com.yourstore.app.data.common

/**
 * Обертка для данных, получаемых из репозитория.
 * Представляет 3 состояния: загрузка, успех, ошибка.
 */
sealed class Resource<out T> {
    class Loading<T> : Resource<T>()
    data class Success<T>(val data: T?) : Resource<T>()
    data class Error<T>(val message: String?, val data: T? = null) : Resource<T>()

    companion object {
        fun <T> loading(): Resource<T> = Loading()
        fun <T> success(data: T?): Resource<T> = Success(data)
        fun <T> error(message: String?, data: T? = null): Resource<T> = Error(message, data)
    }
}
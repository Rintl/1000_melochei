package com.example.a1000_melochei.data.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Абстрактный класс для реализации шаблона NetworkBoundResource.
 * Позволяет получать данные из локального кэша и при необходимости обновлять их из сети.
 */
abstract class NetworkBoundResource<ResultType, RequestType> {

    fun asFlow() = flow {
        // Сначала загружаем данные из кэша
        val dbSource = loadFromDb()
        emit(Resource.Loading())

        // Получаем первое значение из потока для проверки необходимости обновления
        val firstDbValue = dbSource.first()

        if (shouldFetch(firstDbValue)) {
            // Если данные нужно обновить из сети
            try {
                // Выполняем сетевой запрос
                val apiResponse = fetchFromNetwork()
                // Сохраняем результат в кэш
                saveNetworkResult(apiResponse)
                // Возвращаем обновленные данные из кэша
                emitAll(loadFromDb().map { Resource.Success(it) })
            } catch (e: Exception) {
                // В случае ошибки загружаем данные из кэша, если они есть
                emit(Resource.Error(e.message ?: "Неизвестная ошибка", firstDbValue))
            }
        } else {
            // Если обновление не требуется, возвращаем кэшированные данные
            emitAll(loadFromDb().map { Resource.Success(it) })
        }
    }

    /**
     * Загрузка данных из локального кэша
     */
    protected abstract fun loadFromDb(): Flow<ResultType>

    /**
     * Определение необходимости обновления данных из сети
     * @param data Данные из локального кэша
     */
    protected abstract fun shouldFetch(data: ResultType?): Boolean

    /**
     * Получение данных из сети
     */
    protected abstract suspend fun fetchFromNetwork(): RequestType

    /**
     * Сохранение данных, полученных из сети, в локальный кэш
     */
    protected abstract suspend fun saveNetworkResult(data: RequestType)
}
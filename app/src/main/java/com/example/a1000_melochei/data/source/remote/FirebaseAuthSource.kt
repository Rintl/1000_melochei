package com.yourstore.app.data.source.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.yourstore.app.data.common.Resource
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

/**
 * Источник данных для работы с Firebase Authentication.
 * Предоставляет методы для регистрации, входа и управления аккаунтом пользователя.
 */
class FirebaseAuthSource(private val firebaseAuth: FirebaseAuth) {

    /**
     * Текущий ID пользователя или null, если пользователь не авторизован
     */
    val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    /**
     * Проверка, авторизован ли пользователь
     */
    val isUserAuthenticated: Boolean
        get() = firebaseAuth.currentUser != null

    /**
     * Регистрация нового пользователя
     * @param email Email пользователя
     * @param password Пароль пользователя
     * @return Resource с ID нового пользователя
     */
    suspend fun register(email: String, password: String): Resource<String> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid
                ?: return Resource.Error("Ошибка регистрации: не удалось получить ID пользователя")
            Resource.Success(userId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(parseAuthError(e))
        }
    }

    /**
     * Вход пользователя
     * @param email Email пользователя
     * @param password Пароль пользователя
     * @return Resource с ID авторизованного пользователя
     */
    suspend fun login(email: String, password: String): Resource<String> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid
                ?: return Resource.Error("Ошибка входа: не удалось получить ID пользователя")
            Resource.Success(userId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(parseAuthError(e))
        }
    }

    /**
     * Выход из аккаунта
     */
    fun logout() {
        firebaseAuth.signOut()
    }

    /**
     * Сброс пароля
     * @param email Email пользователя
     * @return Resource с результатом операции
     */
    suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(parseAuthError(e))
        }
    }

    /**
     * Изменение пароля для авторизованного пользователя
     * @param newPassword Новый пароль
     * @return Resource с результатом операции
     */
    suspend fun updatePassword(newPassword: String): Resource<Unit> {
        val user = firebaseAuth.currentUser
            ?: return Resource.Error("Пользователь не авторизован")

        return try {
            user.updatePassword(newPassword).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(parseAuthError(e))
        }
    }

    /**
     * Повторная аутентификация пользователя (для операций, требующих недавней аутентификации)
     * @param email Email пользователя
     * @param password Текущий пароль
     * @return Resource с результатом операции
     */
    suspend fun reauthenticate(email: String, password: String): Resource<Unit> {
        val user = firebaseAuth.currentUser
            ?: return Resource.Error("Пользователь не авторизован")

        return try {
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(parseAuthError(e))
        }
    }

    /**
     * Обновление профиля пользователя
     * @param displayName Отображаемое имя пользователя
     * @return Resource с результатом операции
     */
    suspend fun updateProfile(displayName: String): Resource<Unit> {
        val user = firebaseAuth.currentUser
            ?: return Resource.Error("Пользователь не авторизован")

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        return try {
            user.updateProfile(profileUpdates).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(parseAuthError(e))
        }
    }

    /**
     * Парсинг ошибок Firebase Auth для пользовательского отображения
     */
    private fun parseAuthError(e: Exception): String {
        return when {
            e.message?.contains("email address is badly formatted") == true ->
                "Неверный формат электронной почты"
            e.message?.contains("password is invalid") == true ->
                "Неверный пароль"
            e.message?.contains("no user record") == true ->
                "Пользователь с таким email не найден"
            e.message?.contains("email address is already in use") == true ->
                "Этот email уже используется"
            e.message?.contains("password should be at least 6 characters") == true ->
                "Пароль должен содержать минимум 6 символов"
            e.message?.contains("network error") == true ->
                "Проблема с подключением к сети. Проверьте интернет-соединение"
            else -> e.message ?: "Неизвестная ошибка авторизации"
        }
    }
}
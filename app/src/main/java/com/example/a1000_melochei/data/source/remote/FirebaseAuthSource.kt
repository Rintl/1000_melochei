package com.example.a1000_melochei.data.source.remote

import android.util.Log
import com.example.a1000_melochei.data.common.Resource
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Источник данных для работы с Firebase Authentication.
 * Управляет авторизацией, регистрацией и другими операциями с пользователями.
 */
class FirebaseAuthSource(
    private val firebaseAuth: FirebaseAuth
) {
    private val TAG = "FirebaseAuthSource"

    /**
     * Создает нового пользователя с email и паролем
     */
    suspend fun createUser(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                Log.d(TAG, "Пользователь создан: ${user.email}")
                Resource.Success(user)
            } else {
                Resource.Error("Не удалось создать пользователя")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка создания пользователя: ${e.message}", e)
            Resource.Error(getAuthErrorMessage(e))
        }
    }

    /**
     * Авторизует пользователя с email и паролем
     */
    suspend fun signInUser(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                Log.d(TAG, "Пользователь авторизован: ${user.email}")
                Resource.Success(user)
            } else {
                Resource.Error("Не удалось авторизовать пользователя")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка авторизации пользователя: ${e.message}", e)
            Resource.Error(getAuthErrorMessage(e))
        }
    }

    /**
     * Выходит из системы
     */
    suspend fun signOut(): Resource<Unit> {
        return try {
            firebaseAuth.signOut()
            Log.d(TAG, "Пользователь вышел из системы")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка выхода из системы: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка выхода из системы")
        }
    }

    /**
     * Отправляет письмо для сброса пароля
     */
    suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Письмо для сброса пароля отправлено: $email")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отправки письма для сброса пароля: ${e.message}", e)
            Resource.Error(getAuthErrorMessage(e))
        }
    }

    /**
     * Изменяет пароль пользователя
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Resource<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user?.email == null) {
                return Resource.Error("Пользователь не авторизован")
            }

            // Переавторизация для изменения пароля
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential).await()

            // Изменение пароля
            user.updatePassword(newPassword).await()

            Log.d(TAG, "Пароль изменен для пользователя: ${user.email}")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка изменения пароля: ${e.message}", e)
            Resource.Error(getAuthErrorMessage(e))
        }
    }

    /**
     * Обновляет email пользователя
     */
    suspend fun updateEmail(newEmail: String, currentPassword: String): Resource<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user?.email == null) {
                return Resource.Error("Пользователь не авторизован")
            }

            // Переавторизация для изменения email
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential).await()

            // Изменение email
            user.updateEmail(newEmail).await()

            Log.d(TAG, "Email изменен для пользователя: ${user.email} -> $newEmail")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка изменения email: ${e.message}", e)
            Resource.Error(getAuthErrorMessage(e))
        }
    }

    /**
     * Отправляет письмо для подтверждения email
     */
    suspend fun sendEmailVerification(): Resource<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user == null) {
                return Resource.Error("Пользователь не авторизован")
            }

            user.sendEmailVerification().await()
            Log.d(TAG, "Письмо для подтверждения email отправлено: ${user.email}")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отправки письма для подтверждения email: ${e.message}", e)
            Resource.Error(getAuthErrorMessage(e))
        }
    }

    /**
     * Перезагружает данные текущего пользователя
     */
    suspend fun reloadUser(): Resource<FirebaseUser> {
        return try {
            val user = firebaseAuth.currentUser
            if (user == null) {
                return Resource.Error("Пользователь не авторизован")
            }

            user.reload().await()
            Log.d(TAG, "Данные пользователя перезагружены: ${user.email}")
            Resource.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка перезагрузки данных пользователя: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка перезагрузки данных пользователя")
        }
    }

    /**
     * Удаляет аккаунт пользователя
     */
    suspend fun deleteUser(): Resource<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user == null) {
                return Resource.Error("Пользователь не авторизован")
            }

            val email = user.email
            user.delete().await()

            Log.d(TAG, "Аккаунт пользователя удален: $email")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления аккаунта пользователя: ${e.message}", e)
            Resource.Error(getAuthErrorMessage(e))
        }
    }

    /**
     * Получает текущего пользователя
     */
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    /**
     * Проверяет, авторизован ли пользователь
     */
    fun isUserSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Получает UID текущего пользователя
     */
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    /**
     * Получает email текущего пользователя
     */
    fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    /**
     * Проверяет, подтвержден ли email пользователя
     */
    fun isEmailVerified(): Boolean {
        return firebaseAuth.currentUser?.isEmailVerified == true
    }

    /**
     * Получает поток состояния авторизации
     */
    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    /**
     * Получает ID токен текущего пользователя
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): Resource<String> {
        return try {
            val user = firebaseAuth.currentUser
            if (user == null) {
                return Resource.Error("Пользователь не авторизован")
            }

            val tokenResult = user.getIdToken(forceRefresh).await()
            val token = tokenResult.token

            if (token != null) {
                Log.d(TAG, "ID токен получен для пользователя: ${user.email}")
                Resource.Success(token)
            } else {
                Resource.Error("Не удалось получить ID токен")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения ID токена: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка получения ID токена")
        }
    }

    /**
     * Привязывает учетные данные к аккаунту
     */
    suspend fun linkCredential(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val user = firebaseAuth.currentUser
            if (user == null) {
                return Resource.Error("Пользователь не авторизован")
            }

            val credential = EmailAuthProvider.getCredential(email, password)
            val authResult = user.linkWithCredential(credential).await()
            val linkedUser = authResult.user

            if (linkedUser != null) {
                Log.d(TAG, "Учетные данные привязаны к аккаунту: $email")
                Resource.Success(linkedUser)
            } else {
                Resource.Error("Не удалось привязать учетные данные")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка привязки учетных данных: ${e.message}", e)
            Resource.Error(getAuthErrorMessage(e))
        }
    }

    /**
     * Отвязывает провайдера от аккаунта
     */
    suspend fun unlinkProvider(providerId: String): Resource<FirebaseUser> {
        return try {
            val user = firebaseAuth.currentUser
            if (user == null) {
                return Resource.Error("Пользователь не авторизован")
            }

            val authResult = user.unlink(providerId).await()
            val unlinkedUser = authResult.user

            if (unlinkedUser != null) {
                Log.d(TAG, "Провайдер отвязан от аккаунта: $providerId")
                Resource.Success(unlinkedUser)
            } else {
                Resource.Error("Не удалось отвязать провайдера")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отвязки провайдера: ${e.message}", e)
            Resource.Error(getAuthErrorMessage(e))
        }
    }

    /**
     * Проверяет действительность сессии пользователя
     */
    suspend fun checkAuthState(): Resource<FirebaseUser?> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Перезагружаем данные пользователя для проверки актуальности
                user.reload().await()
                Log.d(TAG, "Сессия пользователя действительна: ${user.email}")
                Resource.Success(user)
            } else {
                Log.d(TAG, "Пользователь не авторизован")
                Resource.Success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки состояния авторизации: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка проверки состояния авторизации")
        }
    }

    /**
     * Получает список провайдеров для email
     */
    suspend fun getSignInMethodsForEmail(email: String): Resource<List<String>> {
        return try {
            val signInMethods = firebaseAuth.fetchSignInMethodsForEmail(email).await()
            val methods = signInMethods.signInMethods ?: emptyList()

            Log.d(TAG, "Методы входа для $email: $methods")
            Resource.Success(methods)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения методов входа для email: ${e.message}", e)
            Resource.Error(getAuthErrorMessage(e))
        }
    }

    /**
     * Преобразует исключения Firebase Auth в понятные сообщения
     */
    private fun getAuthErrorMessage(exception: Exception): String {
        val errorCode = when (exception.message) {
            null -> "unknown"
            else -> {
                when {
                    exception.message!!.contains("ERROR_INVALID_EMAIL") -> "invalid-email"
                    exception.message!!.contains("ERROR_WRONG_PASSWORD") -> "wrong-password"
                    exception.message!!.contains("ERROR_USER_NOT_FOUND") -> "user-not-found"
                    exception.message!!.contains("ERROR_USER_DISABLED") -> "user-disabled"
                    exception.message!!.contains("ERROR_TOO_MANY_REQUESTS") -> "too-many-requests"
                    exception.message!!.contains("ERROR_OPERATION_NOT_ALLOWED") -> "operation-not-allowed"
                    exception.message!!.contains("ERROR_EMAIL_ALREADY_IN_USE") -> "email-already-in-use"
                    exception.message!!.contains("ERROR_WEAK_PASSWORD") -> "weak-password"
                    exception.message!!.contains("ERROR_REQUIRES_RECENT_LOGIN") -> "requires-recent-login"
                    exception.message!!.contains("ERROR_CREDENTIAL_ALREADY_IN_USE") -> "credential-already-in-use"
                    exception.message!!.contains("ERROR_INVALID_CREDENTIAL") -> "invalid-credential"
                    else -> "unknown"
                }
            }
        }

        return when (errorCode) {
            "invalid-email" -> "Некорректный email адрес"
            "wrong-password" -> "Неверный пароль"
            "user-not-found" -> "Пользователь с таким email не найден"
            "user-disabled" -> "Аккаунт пользователя отключен"
            "too-many-requests" -> "Слишком много попыток. Попробуйте позже"
            "operation-not-allowed" -> "Операция не разрешена"
            "email-already-in-use" -> "Email уже используется другим аккаунтом"
            "weak-password" -> "Слишком слабый пароль"
            "requires-recent-login" -> "Требуется повторная авторизация"
            "credential-already-in-use" -> "Учетные данные уже используются"
            "invalid-credential" -> "Недействительные учетные данные"
            else -> exception.message ?: "Неизвестная ошибка авторизации"
        }
    }
}
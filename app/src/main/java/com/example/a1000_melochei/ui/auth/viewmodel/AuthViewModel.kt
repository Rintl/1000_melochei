package com.example.a1000_melochei.ui.auth.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.User
import com.example.a1000_melochei.data.repository.UserRepository
import com.example.a1000_melochei.util.Constants
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * ViewModel для управления авторизацией и регистрацией пользователей
 */
class AuthViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val TAG = "AuthViewModel"

    // LiveData для результата авторизации
    private val _loginResult = MutableLiveData<Resource<User>>()
    val loginResult: LiveData<Resource<User>> = _loginResult

    // LiveData для результата регистрации
    private val _registerResult = MutableLiveData<Resource<User>>()
    val registerResult: LiveData<Resource<User>> = _registerResult

    // LiveData для результата авторизации администратора
    private val _adminLoginResult = MutableLiveData<Resource<User>>()
    val adminLoginResult: LiveData<Resource<User>> = _adminLoginResult

    // LiveData для результата выхода
    private val _logoutResult = MutableLiveData<Resource<Unit>>()
    val logoutResult: LiveData<Resource<Unit>> = _logoutResult

    // LiveData для результата сброса пароля
    private val _resetPasswordResult = MutableLiveData<Resource<Unit>>()
    val resetPasswordResult: LiveData<Resource<Unit>> = _resetPasswordResult

    // LiveData для валидации формы входа
    private val _loginFormValid = MutableLiveData<Boolean>()
    val loginFormValid: LiveData<Boolean> = _loginFormValid

    // LiveData для валидации формы регистрации
    private val _registerFormValid = MutableLiveData<Boolean>()
    val registerFormValid: LiveData<Boolean> = _registerFormValid

    // LiveData для валидации формы администратора
    private val _adminFormValid = MutableLiveData<Boolean>()
    val adminFormValid: LiveData<Boolean> = _adminFormValid

    // LiveData для текущего пользователя
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    // Данные формы для валидации
    private var loginEmail: String = ""
    private var loginPassword: String = ""
    private var registerEmail: String = ""
    private var registerPassword: String = ""
    private var registerConfirmPassword: String = ""
    private var registerName: String = ""
    private var registerPhone: String = ""
    private var adminEmail: String = ""
    private var adminPassword: String = ""
    private var adminCode: String = ""

    init {
        // Проверяем, есть ли авторизованный пользователь при инициализации
        checkCurrentUser()
    }

    /**
     * Авторизует пользователя
     */
    fun loginUser(email: String, password: String) {
        _loginResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.loginUser(email.trim(), password)
                _loginResult.value = result

                if (result is Resource.Success) {
                    _currentUser.value = result.data
                    Log.d(TAG, "Пользователь авторизован: ${result.data.email}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при авторизации: ${e.message}", e)
                _loginResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Регистрирует нового пользователя
     */
    fun registerUser(
        email: String,
        password: String,
        confirmPassword: String,
        name: String,
        phone: String
    ) {
        _registerResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Дополнительная валидация
                if (password != confirmPassword) {
                    _registerResult.value = Resource.Error("Пароли не совпадают")
                    return@launch
                }

                val result = userRepository.registerUser(
                    email.trim(),
                    password,
                    name.trim(),
                    phone.trim()
                )
                _registerResult.value = result

                if (result is Resource.Success) {
                    _currentUser.value = result.data
                    Log.d(TAG, "Пользователь зарегистрирован: ${result.data.email}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при регистрации: ${e.message}", e)
                _registerResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Авторизует администратора
     */
    fun loginAdmin(email: String, password: String, adminCode: String) {
        _adminLoginResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.loginAdmin(email.trim(), password, adminCode)
                _adminLoginResult.value = result

                if (result is Resource.Success) {
                    _currentUser.value = result.data
                    Log.d(TAG, "Администратор авторизован: ${result.data.email}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при авторизации администратора: ${e.message}", e)
                _adminLoginResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Выходит из системы
     */
    fun logout() {
        _logoutResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.logout()
                _logoutResult.value = result

                if (result is Resource.Success) {
                    _currentUser.value = null
                    clearFormData()
                    Log.d(TAG, "Пользователь вышел из системы")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при выходе: ${e.message}", e)
                _logoutResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Отправляет письмо для сброса пароля
     */
    fun resetPassword(email: String) {
        _resetPasswordResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.resetPassword(email.trim())
                _resetPasswordResult.value = result

                if (result is Resource.Success) {
                    Log.d(TAG, "Письмо для сброса пароля отправлено: $email")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при сбросе пароля: ${e.message}", e)
                _resetPasswordResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Проверяет текущего авторизованного пользователя
     */
    fun checkCurrentUser() {
        viewModelScope.launch {
            try {
                val result = userRepository.getCurrentUser()
                if (result is Resource.Success) {
                    _currentUser.value = result.data
                    Log.d(TAG, "Текущий пользователь: ${result.data?.email ?: "нет"}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при проверке текущего пользователя: ${e.message}", e)
                _currentUser.value = null
            }
        }
    }

    /**
     * Обновляет данные формы входа и валидирует их
     */
    fun updateLoginForm(email: String, password: String) {
        loginEmail = email
        loginPassword = password
        validateLoginForm()
    }

    /**
     * Обновляет данные формы регистрации и валидирует их
     */
    fun updateRegisterForm(
        email: String,
        password: String,
        confirmPassword: String,
        name: String,
        phone: String
    ) {
        registerEmail = email
        registerPassword = password
        registerConfirmPassword = confirmPassword
        registerName = name
        registerPhone = phone
        validateRegisterForm()
    }

    /**
     * Обновляет данные формы администратора и валидирует их
     */
    fun updateAdminForm(email: String, password: String, adminCode: String) {
        adminEmail = email
        adminPassword = password
        adminCode = adminCode
        validateAdminForm()
    }

    /**
     * Валидирует форму входа
     */
    private fun validateLoginForm() {
        val isEmailValid = isValidEmail(loginEmail)
        val isPasswordValid = loginPassword.isNotBlank()

        val isValid = isEmailValid && isPasswordValid
        _loginFormValid.value = isValid

        Log.d(TAG, "Валидация формы входа: email=$isEmailValid, password=$isPasswordValid, valid=$isValid")
    }

    /**
     * Валидирует форму регистрации
     */
    private fun validateRegisterForm() {
        val isEmailValid = isValidEmail(registerEmail)
        val isPasswordValid = isValidPassword(registerPassword)
        val isConfirmPasswordValid = registerPassword == registerConfirmPassword && registerConfirmPassword.isNotBlank()
        val isNameValid = isValidName(registerName)
        val isPhoneValid = isValidPhone(registerPhone)

        val isValid = isEmailValid && isPasswordValid && isConfirmPasswordValid && isNameValid && isPhoneValid
        _registerFormValid.value = isValid

        Log.d(TAG, "Валидация формы регистрации: email=$isEmailValid, password=$isPasswordValid, " +
                "confirmPassword=$isConfirmPasswordValid, name=$isNameValid, phone=$isPhoneValid, valid=$isValid")
    }

    /**
     * Валидирует форму администратора
     */
    private fun validateAdminForm() {
        val isEmailValid = isValidEmail(adminEmail)
        val isPasswordValid = adminPassword.isNotBlank()
        val isCodeValid = adminCode.isNotBlank()

        val isValid = isEmailValid && isPasswordValid && isCodeValid
        _adminFormValid.value = isValid

        Log.d(TAG, "Валидация формы админа: email=$isEmailValid, password=$isPasswordValid, code=$isCodeValid, valid=$isValid")
    }

    /**
     * Проверяет валидность email
     */
    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() &&
                Pattern.compile(Constants.EMAIL_REGEX).matcher(email).matches()
    }

    /**
     * Проверяет валидность пароля
     */
    private fun isValidPassword(password: String): Boolean {
        return password.length >= Constants.MIN_PASSWORD_LENGTH
    }

    /**
     * Проверяет валидность имени
     */
    private fun isValidName(name: String): Boolean {
        return name.isNotBlank() &&
                name.trim().length >= 2 &&
                name.trim().length <= 50
    }

    /**
     * Проверяет валидность номера телефона
     */
    private fun isValidPhone(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^+\\d]"), "")
        return cleanPhone.length >= Constants.MIN_PHONE_LENGTH &&
                Pattern.compile(Constants.PHONE_REGEX).matcher(cleanPhone).matches()
    }

    /**
     * Получает детальные ошибки валидации для email
     */
    fun getEmailValidationError(email: String): String? {
        return when {
            email.isBlank() -> "Email не может быть пустым"
            !isValidEmail(email) -> "Некорректный формат email"
            else -> null
        }
    }

    /**
     * Получает детальные ошибки валидации для пароля
     */
    fun getPasswordValidationError(password: String): String? {
        return when {
            password.isBlank() -> "Пароль не может быть пустым"
            password.length < Constants.MIN_PASSWORD_LENGTH -> "Пароль должен содержать не менее ${Constants.MIN_PASSWORD_LENGTH} символов"
            else -> null
        }
    }

    /**
     * Получает детальные ошибки валидации для подтверждения пароля
     */
    fun getConfirmPasswordValidationError(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Подтвердите пароль"
            password != confirmPassword -> "Пароли не совпадают"
            else -> null
        }
    }

    /**
     * Получает детальные ошибки валидации для имени
     */
    fun getNameValidationError(name: String): String? {
        return when {
            name.isBlank() -> "Имя не может быть пустым"
            name.trim().length < 2 -> "Имя должно содержать не менее 2 символов"
            name.trim().length > 50 -> "Имя должно содержать не более 50 символов"
            else -> null
        }
    }

    /**
     * Получает детальные ошибки валидации для телефона
     */
    fun getPhoneValidationError(phone: String): String? {
        val cleanPhone = phone.replace(Regex("[^+\\d]"), "")
        return when {
            phone.isBlank() -> "Номер телефона не может быть пустым"
            cleanPhone.length < Constants.MIN_PHONE_LENGTH -> "Некорректный номер телефона"
            !Pattern.compile(Constants.PHONE_REGEX).matcher(cleanPhone).matches() -> "Некорректный формат номера телефона"
            else -> null
        }
    }

    /**
     * Проверяет, авторизован ли пользователь
     */
    fun isUserLoggedIn(): Boolean {
        return userRepository.isUserLoggedIn()
    }

    /**
     * Проверяет, является ли пользователь администратором
     */
    fun isUserAdmin(): Boolean {
        return _currentUser.value?.isAdmin == true
    }

    /**
     * Получает поток состояния авторизации
     */
    fun getAuthStateFlow(): Flow<FirebaseUser?> {
        return userRepository.getAuthStateFlow()
    }

    /**
     * Очищает результаты операций
     */
    fun clearResults() {
        _loginResult.value = Resource.Loading()
        _registerResult.value = Resource.Loading()
        _adminLoginResult.value = Resource.Loading()
        _logoutResult.value = Resource.Loading()
        _resetPasswordResult.value = Resource.Loading()
    }

    /**
     * Очищает данные форм
     */
    private fun clearFormData() {
        loginEmail = ""
        loginPassword = ""
        registerEmail = ""
        registerPassword = ""
        registerConfirmPassword = ""
        registerName = ""
        registerPhone = ""
        adminEmail = ""
        adminPassword = ""
        adminCode = ""

        _loginFormValid.value = false
        _registerFormValid.value = false
        _adminFormValid.value = false
    }

    /**
     * Форматирует номер телефона для отображения
     */
    fun formatPhoneNumber(phone: String): String {
        val cleanPhone = phone.replace(Regex("[^\\d]"), "")
        return when {
            cleanPhone.length >= 11 && cleanPhone.startsWith("7") -> {
                "+7 (${cleanPhone.substring(1, 4)}) ${cleanPhone.substring(4, 7)}-${cleanPhone.substring(7, 9)}-${cleanPhone.substring(9, minOf(11, cleanPhone.length))}"
            }
            cleanPhone.length >= 10 -> {
                "+${cleanPhone.substring(0, 1)} (${cleanPhone.substring(1, 4)}) ${cleanPhone.substring(4, 7)}-${cleanPhone.substring(7, 9)}-${cleanPhone.substring(9, minOf(11, cleanPhone.length))}"
            }
            else -> phone
        }
    }

    /**
     * Переключает видимость пароля (для UI)
     */
    private val _passwordVisible = MutableLiveData<Boolean>(false)
    val passwordVisible: LiveData<Boolean> = _passwordVisible

    fun togglePasswordVisibility() {
        _passwordVisible.value = !(_passwordVisible.value ?: false)
    }
}
package com.example.a1000_melochei.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.databinding.ActivityLoginBinding
import com.example.a1000_melochei.ui.admin.AdminActivity
import com.example.a1000_melochei.ui.auth.viewmodel.AuthViewModel
import com.example.a1000_melochei.ui.customer.CustomerActivity
import com.example.a1000_melochei.util.showToast
import com.example.a1000_melochei.util.hideKeyboard
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Активность для авторизации пользователей.
 * Предоставляет интерфейс для входа в систему и перехода к регистрации.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        setupListeners()
    }

    /**
     * Настраивает пользовательский интерфейс
     */
    private fun setupUI() {
        // Скрываем ActionBar
        supportActionBar?.hide()

        // Устанавливаем начальное состояние
        updateLoginButtonState(false)
    }

    /**
     * Настраивает слушатели событий
     */
    private fun setupListeners() {
        // Слушатели изменения текста
        binding.etEmail.doOnTextChanged { text, _, _, _ ->
            updateFormValidation()
            clearEmailError()
        }

        binding.etPassword.doOnTextChanged { text, _, _, _ ->
            updateFormValidation()
            clearPasswordError()
        }

        // Кнопка входа
        binding.btnLogin.setOnClickListener {
            hideKeyboard()
            performLogin()
        }

        // Ссылка на регистрацию
        binding.tvRegister.setOnClickListener {
            navigateToRegister()
        }

        // Ссылка на восстановление пароля
        binding.tvForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }

        // Ссылка на вход администратора
        binding.tvAdminLogin.setOnClickListener {
            navigateToAdminLogin()
        }
    }

    /**
     * Настраивает наблюдателей ViewModel
     */
    private fun setupObservers() {
        // Наблюдаем за валидацией формы
        authViewModel.loginFormValid.observe(this) { isValid ->
            updateLoginButtonState(isValid)
        }

        // Наблюдаем за результатом авторизации
        authViewModel.loginResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoadingState(true)
                }
                is Resource.Success -> {
                    showLoadingState(false)
                    showToast(getString(R.string.login_success))

                    // Переходим к соответствующему экрану
                    val user = resource.data
                    if (user.isAdmin) {
                        navigateToAdmin()
                    } else {
                        navigateToCustomer()
                    }
                }
                is Resource.Error -> {
                    showLoadingState(false)
                    handleLoginError(resource.message)
                }
            }
        }
    }

    /**
     * Обновляет валидацию формы
     */
    private fun updateFormValidation() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()

        authViewModel.updateLoginForm(email, password)
    }

    /**
     * Выполняет авторизацию пользователя
     */
    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Дополнительная валидация
        if (!validateInput(email, password)) {
            return
        }

        // Очищаем предыдущие результаты
        authViewModel.clearResults()

        // Выполняем авторизацию
        authViewModel.loginUser(email, password)
    }

    /**
     * Валидирует введенные данные
     */
    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        // Проверяем email
        val emailError = authViewModel.getEmailValidationError(email)
        if (emailError != null) {
            binding.tilEmail.error = emailError
            isValid = false
        }

        // Проверяем пароль
        val passwordError = authViewModel.getPasswordValidationError(password)
        if (passwordError != null) {
            binding.tilPassword.error = passwordError
            isValid = false
        }

        return isValid
    }

    /**
     * Обрабатывает ошибки авторизации
     */
    private fun handleLoginError(errorMessage: String?) {
        val message = errorMessage ?: getString(R.string.auth_failed)

        when {
            message.contains("email", ignoreCase = true) -> {
                binding.tilEmail.error = message
            }
            message.contains("password", ignoreCase = true) -> {
                binding.tilPassword.error = message
            }
            else -> {
                showToast(message)
            }
        }
    }

    /**
     * Обновляет состояние кнопки входа
     */
    private fun updateLoginButtonState(isEnabled: Boolean) {
        binding.btnLogin.isEnabled = isEnabled
        binding.btnLogin.alpha = if (isEnabled) 1.0f else 0.5f
    }

    /**
     * Показывает/скрывает состояние загрузки
     */
    private fun showLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading

        if (isLoading) {
            binding.btnLogin.text = getString(R.string.loading)
        } else {
            binding.btnLogin.text = getString(R.string.login)
        }
    }

    /**
     * Очищает ошибку поля email
     */
    private fun clearEmailError() {
        binding.tilEmail.error = null
    }

    /**
     * Очищает ошибку поля пароля
     */
    private fun clearPasswordError() {
        binding.tilPassword.error = null
    }

    /**
     * Переходит к экрану регистрации
     */
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    /**
     * Переходит к экрану восстановления пароля
     */
    private fun navigateToForgotPassword() {
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        val email = binding.etEmail.text.toString().trim()
        if (email.isNotEmpty()) {
            intent.putExtra("email", email)
        }
        startActivity(intent)
    }

    /**
     * Переходит к экрану входа администратора
     */
    private fun navigateToAdminLogin() {
        val intent = Intent(this, AdminLoginActivity::class.java)
        val email = binding.etEmail.text.toString().trim()
        if (email.isNotEmpty()) {
            intent.putExtra("email", email)
        }
        startActivity(intent)
    }

    /**
     * Переходит к клиентской части приложения
     */
    private fun navigateToCustomer() {
        val intent = Intent(this, CustomerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Переходит к административной части приложения
     */
    private fun navigateToAdmin() {
        val intent = Intent(this, AdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Обрабатывает нажатие кнопки "Назад"
     */
    override fun onBackPressed() {
        // Закрываем приложение при нажатии "Назад" на экране авторизации
        finishAffinity()
    }

    /**
     * Очищает наблюдателей при уничтожении активности
     */
    override fun onDestroy() {
        super.onDestroy()
        authViewModel.loginResult.removeObservers(this)
        authViewModel.loginFormValid.removeObservers(this)
    }
}
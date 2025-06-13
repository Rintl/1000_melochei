package com.example.a1000_melochei.ui.auth

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.databinding.ActivityForgotPasswordBinding
import com.example.a1000_melochei.ui.auth.viewmodel.AuthViewModel
import com.example.a1000_melochei.util.showToast
import com.example.a1000_melochei.util.hideKeyboard
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Активность для восстановления пароля.
 * Позволяет пользователю запросить отправку письма для сброса пароля.
 */
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val authViewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        setupListeners()
        handleIntent()
    }

    /**
     * Настраивает пользовательский интерфейс
     */
    private fun setupUI() {
        // Настраиваем ActionBar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.forgot_password_title)
        }

        // Устанавливаем начальное состояние
        updateResetButtonState(false)

        // Устанавливаем описание
        binding.tvDescription.text = getString(R.string.forgot_password_description)
    }

    /**
     * Обрабатывает переданные данные
     */
    private fun handleIntent() {
        // Если email был передан из предыдущего экрана
        val email = intent.getStringExtra("email")
        if (!email.isNullOrEmpty()) {
            binding.etEmail.setText(email)
            updateFormValidation()
        }
    }

    /**
     * Настраивает слушатели событий
     */
    private fun setupListeners() {
        // Слушатель изменения текста email
        binding.etEmail.doOnTextChanged { _, _, _, _ ->
            updateFormValidation()
            clearEmailError()
        }

        // Кнопка отправки
        binding.btnResetPassword.setOnClickListener {
            hideKeyboard()
            performPasswordReset()
        }

        // Ссылка на возврат к авторизации
        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    /**
     * Настраивает наблюдателей ViewModel
     */
    private fun setupObservers() {
        // Наблюдаем за результатом сброса пароля
        authViewModel.resetPasswordResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoadingState(true)
                }
                is Resource.Success -> {
                    showLoadingState(false)
                    showSuccessState()
                }
                is Resource.Error -> {
                    showLoadingState(false)
                    handleResetError(resource.message)
                }
            }
        }
    }

    /**
     * Обновляет валидацию формы
     */
    private fun updateFormValidation() {
        val email = binding.etEmail.text.toString().trim()
        val isValid = authViewModel.getEmailValidationError(email) == null && email.isNotEmpty()
        updateResetButtonState(isValid)
    }

    /**
     * Выполняет сброс пароля
     */
    private fun performPasswordReset() {
        val email = binding.etEmail.text.toString().trim()

        // Валидация email
        val emailError = authViewModel.getEmailValidationError(email)
        if (emailError != null) {
            binding.tilEmail.error = emailError
            return
        }

        // Очищаем предыдущие результаты
        authViewModel.clearResults()

        // Выполняем сброс пароля
        authViewModel.resetPassword(email)
    }

    /**
     * Обрабатывает ошибки сброса пароля
     */
    private fun handleResetError(errorMessage: String?) {
        val message = errorMessage ?: getString(R.string.reset_password_failed)

        if (message.contains("email", ignoreCase = true)) {
            binding.tilEmail.error = message
        } else {
            showToast(message)
        }
    }

    /**
     * Показывает состояние успешной отправки
     */
    private fun showSuccessState() {
        // Скрываем форму
        binding.cardForm.visibility = View.GONE

        // Показываем сообщение об успехе
        binding.cardSuccess.visibility = View.VISIBLE

        val email = binding.etEmail.text.toString().trim()
        binding.tvSuccessMessage.text = getString(R.string.reset_password_success, email)

        // Кнопка возврата к авторизации
        binding.btnBackToLogin.setOnClickListener {
            finish()
        }

        showToast(getString(R.string.reset_password_email_sent))
    }

    /**
     * Обновляет состояние кнопки сброса
     */
    private fun updateResetButtonState(isEnabled: Boolean) {
        binding.btnResetPassword.isEnabled = isEnabled
        binding.btnResetPassword.alpha = if (isEnabled) 1.0f else 0.5f
    }

    /**
     * Показывает/скрывает состояние загрузки
     */
    private fun showLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnResetPassword.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading

        if (isLoading) {
            binding.btnResetPassword.text = getString(R.string.sending)
        } else {
            binding.btnResetPassword.text = getString(R.string.reset_password)
        }
    }

    /**
     * Очищает ошибку поля email
     */
    private fun clearEmailError() {
        binding.tilEmail.error = null
    }

    /**
     * Обрабатывает нажатие кнопки "Назад" в ActionBar
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Очищает наблюдателей при уничтожении активности
     */
    override fun onDestroy() {
        super.onDestroy()
        authViewModel.resetPasswordResult.removeObservers(this)
    }
}
package com.yourstore.app.ui.customer.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.User
import com.yourstore.app.databinding.ActivityEditProfileBinding
import com.yourstore.app.ui.customer.profile.viewmodel.ProfileViewModel
import com.yourstore.app.util.ValidationUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Активность для редактирования профиля пользователя.
 * Позволяет изменить имя, телефон, аватар и пароль.
 */
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: ProfileViewModel by viewModel()

    private var selectedImageUri: Uri? = null
    private var currentUser: User? = null

    // Обработчик результата выбора изображения из галереи
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                loadImageFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupObservers()
        setupListeners()
        setupValidation()

        // Загружаем текущие данные пользователя
        viewModel.loadUserProfile()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.edit_profile)
    }

    private fun setupObservers() {
        // Наблюдение за данными профиля
        viewModel.userProfile.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    result.data?.let { user ->
                        currentUser = user
                        updateUI(user)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_loading_profile),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }

        // Наблюдение за результатом обновления профиля
        viewModel.updateProfileResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.btnSave.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.btnSave.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Error -> {
                    binding.btnSave.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_updating_profile),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Наблюдение за результатом смены пароля
        viewModel.changePasswordResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.btnChangePassword.isEnabled = false
                    binding.changePasswordProgressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.btnChangePassword.isEnabled = true
                    binding.changePasswordProgressBar.visibility = View.GONE

                    // Очищаем поля ввода пароля
                    binding.etCurrentPassword.setText("")
                    binding.etNewPassword.setText("")
                    binding.etConfirmPassword.setText("")

                    // Скрываем форму смены пароля
                    binding.passwordChangeLayout.visibility = View.GONE

                    Toast.makeText(this, R.string.password_changed, Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    binding.btnChangePassword.isEnabled = true
                    binding.changePasswordProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_changing_password),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Валидация формы изменения профиля
        viewModel.profileFormValid.observe(this) { isValid ->
            binding.btnSave.isEnabled = isValid
        }

        // Валидация формы смены пароля
        viewModel.passwordFormValid.observe(this) { isValid ->
            binding.btnChangePassword.isEnabled = isValid
        }
    }

    private fun setupListeners() {
        // Кнопка изменения аватара
        binding.ivUserAvatar.setOnClickListener {
            selectImage()
        }

        binding.btnChangeAvatar.setOnClickListener {
            selectImage()
        }

        // Кнопка "Сменить пароль"
        binding.btnShowPasswordChange.setOnClickListener {
            if (binding.passwordChangeLayout.visibility == View.VISIBLE) {
                binding.passwordChangeLayout.visibility = View.GONE
                binding.btnShowPasswordChange.setText(R.string.change_password)
            } else {
                binding.passwordChangeLayout.visibility = View.VISIBLE
                binding.btnShowPasswordChange.setText(R.string.hide_password_form)
            }
        }

        // Кнопка сохранения изменений профиля
        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        // Кнопка смены пароля
        binding.btnChangePassword.setOnClickListener {
            changePassword()
        }
    }

    private fun setupValidation() {
        // Валидация имени
        binding.etName.doOnTextChanged { text, _, _, _ ->
            binding.tilName.error = if (text.toString().length < 2) {
                getString(R.string.invalid_name)
            } else {
                null
            }
            validateProfileForm()
        }

        // Валидация телефона
        binding.etPhone.doOnTextChanged { text, _, _, _ ->
            binding.tilPhone.error = if (!ValidationUtils.isValidPhone(text.toString())) {
                getString(R.string.invalid_phone)
            } else {
                null
            }
            validateProfileForm()
        }

        // Валидация текущего пароля
        binding.etCurrentPassword.doOnTextChanged { text, _, _, _ ->
            binding.tilCurrentPassword.error = if (text.toString().length < 6) {
                getString(R.string.invalid_password)
            } else {
                null
            }
            validatePasswordForm()
        }

        // Валидация нового пароля
        binding.etNewPassword.doOnTextChanged { text, _, _, _ ->
            binding.tilNewPassword.error = if (!ValidationUtils.isValidPassword(text.toString())) {
                getString(R.string.invalid_password)
            } else {
                null
            }
            validatePasswordForm()
        }

        // Валидация подтверждения пароля
        binding.etConfirmPassword.doOnTextChanged { text, _, _, _ ->
            val newPassword = binding.etNewPassword.text.toString()
            binding.tilConfirmPassword.error = if (text.toString() != newPassword) {
                getString(R.string.passwords_dont_match)
            } else {
                null
            }
            validatePasswordForm()
        }
    }

    private fun updateUI(user: User) {
        binding.etName.setText(user.name)
        binding.etPhone.setText(user.phone)
        binding.tvUserEmail.text = user.email

        // Загрузка аватара пользователя
        if (user.avatarUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.avatarUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(binding.ivUserAvatar)
        } else {
            binding.ivUserAvatar.setImageResource(R.drawable.default_avatar)
        }
    }

    private fun selectImage() {
        val options = arrayOf<CharSequence>(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery),
            getString(R.string.cancel)
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.select_image))
            .setItems(options) { dialog, item ->
                when (options[item]) {
                    getString(R.string.take_photo) -> {
                        // В данной реализации используем только галерею
                        openGallery()
                    }
                    getString(R.string.choose_from_gallery) -> {
                        openGallery()
                    }
                    getString(R.string.cancel) -> {
                        dialog.dismiss()
                    }
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun loadImageFromUri(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .apply(RequestOptions.circleCropTransform())
            .into(binding.ivUserAvatar)
    }

    private fun validateProfileForm() {
        val name = binding.etName.text.toString()
        val phone = binding.etPhone.text.toString()

        viewModel.validateProfileForm(name, phone)
    }

    private fun validatePasswordForm() {
        val currentPassword = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        viewModel.validatePasswordForm(currentPassword, newPassword, confirmPassword)
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString()
        val phone = binding.etPhone.text.toString()

        // Получаем файл изображения, если выбрано новое
        val imageFile = selectedImageUri?.let { uri ->
            try {
                // Создаем временный файл
                val tempFile = File.createTempFile("avatar", ".jpg", cacheDir)

                // Копируем данные из Uri в файл
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(tempFile)

                inputStream?.use { input ->
                    outputStream.use { output ->
                        val buffer = ByteArray(4 * 1024) // 4kb buffer
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                }

                tempFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        // Обновляем профиль
        viewModel.updateProfile(name, phone, imageFile)
    }

    private fun changePassword() {
        val currentPassword = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()

        viewModel.changePassword(currentPassword, newPassword)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.profileFormLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
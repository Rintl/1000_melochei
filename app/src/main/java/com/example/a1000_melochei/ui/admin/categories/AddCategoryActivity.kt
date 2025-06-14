package com.example.a1000_melochei.ui.admin.categories

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Category
import com.example.a1000_melochei.databinding.ActivityAddCategoryBinding
import com.example.a1000_melochei.ui.admin.categories.viewmodel.CategoryViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Активность для добавления или редактирования категории.
 * Позволяет задать название, описание, изображение и настройки видимости категории.
 */
class AddCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCategoryBinding
    private val viewModel: CategoryViewModel by viewModel()

    private var selectedImageUri: Uri? = null
    private var categoryId: String? = null
    private var isEditMode = false
    private var currentCategory: Category? = null

    // Обработчик результата выбора изображения из галереи
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                loadImageFromUri(uri)
            }
        }
    }

    // Обработчик запроса разрешения на доступ к галерее
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(
                this,
                R.string.storage_permission_denied,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверяем, редактируем ли существующую категорию
        categoryId = intent.getStringExtra("CATEGORY_ID")
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)

        setupToolbar()
        setupObservers()
        setupListeners()
        setupValidation()

        // Если редактируем существующую категорию, загружаем её данные
        if (isEditMode && categoryId != null) {
            viewModel.loadCategory(categoryId!!)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) {
            getString(R.string.edit_category)
        } else {
            getString(R.string.add_category)
        }
    }

    private fun setupObservers() {
        // Наблюдение за данными категории (при редактировании)
        viewModel.category.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    result.data?.let { category ->
                        currentCategory = category
                        updateUI(category)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_loading_category),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        })

        // Наблюдение за результатом добавления категории
        viewModel.addCategoryResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.btnSaveCategory.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.btnSaveCategory.isEnabled = true
                    binding.progressBar.visibility = View.GONE

                    val message = if (isEditMode) {
                        R.string.category_updated
                    } else {
                        R.string.category_added
                    }

                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is Resource.Error -> {
                    binding.btnSaveCategory.isEnabled = true
                    binding.progressBar.visibility = View.GONE

                    val errorMessage = if (isEditMode) {
                        result.message ?: getString(R.string.error_updating_category)
                    } else {
                        result.message ?: getString(R.string.error_adding_category)
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Наблюдение за валидностью формы
        viewModel.categoryFormValid.observe(this, Observer { isValid ->
            binding.btnSaveCategory.isEnabled = isValid
        })
    }

    private fun setupListeners() {
        // Кнопка выбора изображения
        binding.ivCategoryImage.setOnClickListener {
            checkStoragePermission()
        }

        binding.btnAddImage.setOnClickListener {
            checkStoragePermission()
        }

        // Кнопка сохранения
        binding.btnSaveCategory.setOnClickListener {
            saveCategory()
        }
    }

    private fun setupValidation() {
        // Валидация названия категории
        binding.etCategoryName.doOnTextChanged { text, _, _, _ ->
            binding.tilCategoryName.error = if (text.toString().length < 2) {
                getString(R.string.invalid_category_name)
            } else {
                null
            }
            validateForm()
        }

        // Валидация описания категории (не обязательное поле)
        binding.etCategoryDescription.doOnTextChanged { _, _, _, _ ->
            validateForm()
        }
    }

    private fun updateUI(category: Category) {
        // Заполняем поля формы данными категории
        binding.etCategoryName.setText(category.name)
        binding.etCategoryDescription.setText(category.description)
        binding.switchVisibility.isChecked = category.isVisible

        // Загружаем изображение категории, если оно есть
        if (category.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(category.imageUrl)
                .apply(RequestOptions.centerCropTransform())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(binding.ivCategoryImage)

            binding.tvNoImage.visibility = View.GONE
        }
    }

    private fun validateForm() {
        val name = binding.etCategoryName.text.toString()

        // Название категории должно быть не короче 2 символов
        val isValid = name.length >= 2

        viewModel.setCategoryFormValid(isValid)
    }

    private fun saveCategory() {
        try {
            val name = binding.etCategoryName.text.toString()
            val description = binding.etCategoryDescription.text.toString()
            val isVisible = binding.switchVisibility.isChecked

            // Получаем файл изображения, если выбрано новое
            val imageFile = selectedImageUri?.let { uri ->
                try {
                    // Создаем временный файл
                    val tempFile = File.createTempFile("category", ".jpg", cacheDir)

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

            if (isEditMode && categoryId != null) {
                // Обновляем существующую категорию
                viewModel.updateCategory(
                    categoryId = categoryId!!,
                    name = name,
                    description = description,
                    isVisible = isVisible,
                    imageFile = imageFile
                )
            } else {
                // Добавляем новую категорию
                viewModel.addCategory(
                    name = name,
                    description = description,
                    isVisible = isVisible,
                    imageFile = imageFile
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkStoragePermission() {
        val readPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, readPermission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, readPermission)) {
                showPermissionRationale()
            } else {
                requestPermissionLauncher.launch(readPermission)
            }
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.storage_permission_explanation)
            .setPositiveButton(R.string.grant) { _, _ ->
                val readPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                requestPermissionLauncher.launch(readPermission)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun loadImageFromUri(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .apply(RequestOptions.centerCropTransform())
            .into(binding.ivCategoryImage)

        binding.tvNoImage.visibility = View.GONE
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.scrollView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
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

    override fun onBackPressed() {
        // Спрашиваем о сохранении изменений только если форма была изменена
        if (isFormModified()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.discard_changes)
                .setMessage(R.string.discard_changes_message)
                .setPositiveButton(R.string.yes) { _, _ -> super.onBackPressed() }
                .setNegativeButton(R.string.no, null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    private fun isFormModified(): Boolean {
        // Если это добавление новой категории и поля не пустые
        if (!isEditMode) {
            val nameNotEmpty = binding.etCategoryName.text.toString().isNotEmpty()
            val descriptionNotEmpty = binding.etCategoryDescription.text.toString().isNotEmpty()
            val hasImage = selectedImageUri != null

            return nameNotEmpty || descriptionNotEmpty || hasImage
        }

        // Если это редактирование существующей категории
        currentCategory?.let { category ->
            val nameChanged = binding.etCategoryName.text.toString() != category.name
            val descriptionChanged = binding.etCategoryDescription.text.toString() != category.description
            val visibilityChanged = binding.switchVisibility.isChecked != category.isVisible
            val hasNewImage = selectedImageUri != null

            return nameChanged || descriptionChanged || visibilityChanged || hasNewImage
        }

        return false
    }
}
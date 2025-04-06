package com.yourstore.app.ui.admin.products

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Category
import com.yourstore.app.databinding.ActivityImportProductsBinding
import com.yourstore.app.ui.admin.products.viewmodel.AdminProductViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Активность для импорта товаров из CSV или Excel файла.
 * Позволяет загрузить файл, выбрать категорию по умолчанию и начать импорт.
 */
class ImportProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportProductsBinding
    private val viewModel: AdminProductViewModel by viewModel()

    private var selectedFileUri: Uri? = null
    private var selectedCategoryId: String? = null
    private var importType: String = "CSV" // По умолчанию CSV

    // Обработчик результата выбора файла
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                updateFileInfo(uri)
            }
        }
    }

    // Обработчик запроса разрешения на доступ к хранилищу
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openFilePicker()
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
        binding = ActivityImportProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupObservers()
        setupListeners()
        setupFileTypeRadioGroup()

        // Загружаем категории
        viewModel.loadCategories()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.import_products)
    }

    private fun setupObservers() {
        // Наблюдение за категориями
        viewModel.categories.observe(this, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    result.data?.let { categories ->
                        setupCategorySpinner(categories)
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_loading_categories),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })

        // Наблюдение за результатом импорта товаров
        viewModel.importProductsResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                    binding.btnStartImport.isEnabled = false
                }
                is Resource.Success -> {
                    showLoading(false)
                    binding.btnStartImport.isEnabled = true

                    result.data?.let { importResult ->
                        showImportResults(importResult)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    binding.btnStartImport.isEnabled = true

                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_importing_products),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun setupListeners() {
        // Кнопка выбора файла
        binding.btnSelectFile.setOnClickListener {
            checkStoragePermission()
        }

        // Кнопка начала импорта
        binding.btnStartImport.setOnClickListener {
            if (selectedFileUri != null) {
                startImport()
            } else {
                Toast.makeText(this, R.string.select_file_first, Toast.LENGTH_SHORT).show()
            }
        }

        // Кнопка скачивания шаблона
        binding.btnDownloadTemplate.setOnClickListener {
            showTemplateSelectionDialog()
        }
    }

    private fun setupCategorySpinner(categories: List<Category>) {
        val categoryNames = ArrayList<String>()
        categoryNames.add(getString(R.string.no_default_category))
        categoryNames.addAll(categories.map { it.name })

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categoryNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDefaultCategory.adapter = adapter

        // Установка слушателя для выбора категории
        binding.spinnerDefaultCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryId = if (position == 0) {
                    null // Нет категории по умолчанию
                } else {
                    categories[position - 1].id
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedCategoryId = null
            }
        }
    }

    private fun setupFileTypeRadioGroup() {
        binding.radioGroupFileType.setOnCheckedChangeListener { _, checkedId ->
            importType = when (checkedId) {
                R.id.radioCSV -> "CSV"
                R.id.radioExcel -> "EXCEL"
                else -> "CSV"
            }

            // Сбрасываем выбранный файл при смене типа
            selectedFileUri = null
            binding.tvSelectedFile.text = getString(R.string.no_file_selected)
            binding.btnStartImport.isEnabled = false
        }
    }

    private fun checkStoragePermission() {
        val readPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, readPermission) == PackageManager.PERMISSION_GRANTED) {
            openFilePicker()
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

    private fun openFilePicker() {
        val mimeType = when (importType) {
            "CSV" -> "text/csv"
            "EXCEL" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        try {
            filePickerLauncher.launch(intent)
        } catch (e: Exception) {
            // Если нет приложения для выбора файлов, покажем более общий выбор
            val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
            }
            filePickerLauncher.launch(fallbackIntent)
        }
    }

    private fun updateFileInfo(uri: Uri) {
        // Получение информации о файле
        val fileName = getFileName(uri)
        binding.tvSelectedFile.text = fileName

        // Проверка соответствия типа файла выбранному типу импорта
        val fileExtension = fileName.substringAfterLast('.', "").lowercase()

        val isValidFile = when (importType) {
            "CSV" -> fileExtension == "csv"
            "EXCEL" -> fileExtension == "xlsx" || fileExtension == "xls"
            else -> false
        }

        if (!isValidFile) {
            Toast.makeText(
                this,
                getString(R.string.wrong_file_format, importType),
                Toast.LENGTH_SHORT
            ).show()
            binding.tvSelectedFile.text = getString(R.string.no_file_selected)
            selectedFileUri = null
            binding.btnStartImport.isEnabled = false
        } else {
            binding.btnStartImport.isEnabled = true
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = "unknown_file"
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            }
        }
        if (result == "unknown_file") {
            result = uri.path?.substringAfterLast('/') ?: "unknown_file"
        }
        return result
    }

    private fun startImport() {
        selectedFileUri?.let { uri ->
            try {
                // Создаем временный файл
                val fileExtension = getFileName(uri).substringAfterLast('.', "")
                val tempFile = File.createTempFile("import", ".$fileExtension", cacheDir)

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

                // Начинаем импорт
                viewModel.importProducts(
                    file = tempFile,
                    fileType = importType,
                    defaultCategoryId = selectedCategoryId,
                    updateExisting = binding.checkboxUpdateExisting.isChecked
                )
            } catch (e: Exception) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showImportResults(result: AdminProductViewModel.ImportResult) {
        val message = StringBuilder()
        message.append(getString(R.string.import_completed)).append("\n\n")
        message.append(getString(R.string.products_imported, result.successCount)).append("\n")
        message.append(getString(R.string.products_updated, result.updatedCount)).append("\n")
        message.append(getString(R.string.products_failed, result.failedCount))

        if (result.errors.isNotEmpty()) {
            message.append("\n\n").append(getString(R.string.errors)).append(":\n")
            result.errors.take(5).forEach { error ->
                message.append("- ").append(error).append("\n")
            }
            if (result.errors.size > 5) {
                message.append("- ").append(getString(R.string.more_errors, result.errors.size - 5))
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.import_results)
            .setMessage(message.toString())
            .setPositiveButton(R.string.ok) { _, _ ->
                if (result.successCount > 0) {
                    setResult(RESULT_OK)
                    finish()
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun showTemplateSelectionDialog() {
        val options = arrayOf<CharSequence>(
            getString(R.string.csv_template),
            getString(R.string.excel_template)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.select_template)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.generateCSVTemplate(this)
                    1 -> viewModel.generateExcelTemplate(this)
                }
            }
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.progressText.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.importForm.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
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
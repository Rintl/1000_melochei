package com.yourstore.app.ui.admin.products

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Category
import com.yourstore.app.data.model.Product
import com.yourstore.app.databinding.ActivityEditProductBinding
import com.yourstore.app.ui.admin.products.adapter.ProductImageAdapter
import com.yourstore.app.ui.admin.products.viewmodel.AdminProductViewModel
import com.yourstore.app.util.CurrencyFormatter
import com.yourstore.app.util.ValidationUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Активность для редактирования существующего товара.
 * Позволяет изменить данные товара, загрузить новые изображения и сохранить изменения.
 */
class EditProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProductBinding
    private val viewModel: AdminProductViewModel by viewModel()

    private lateinit var productImageAdapter: ProductImageAdapter
    private var productId: String = ""
    private var currentProduct: Product? = null
    private var selectedCategoryId: String? = null
    private val selectedImages = mutableListOf<Uri>()
    private val imagesToDelete = mutableListOf<String>()

    // Обработчик результата выбора изображения из галереи
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImages.add(uri)
                updateImagesPreview()
            }
            // Обработка выбора нескольких изображений
            val clipData = result.data?.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    selectedImages.add(clipData.getItemAt(i).uri)
                }
                updateImagesPreview()
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
        binding = ActivityEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем ID товара из переданных данных
        productId = intent.getStringExtra("PRODUCT_ID") ?: ""
        if (productId.isEmpty()) {
            Toast.makeText(this, R.string.error_invalid_product, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupValidation()

        // Загружаем категории и информацию о товаре
        viewModel.loadCategories()
        viewModel.loadProduct(productId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.edit_product)
    }

    private fun setupRecyclerView() {
        // Настройка RecyclerView для изображений товара
        productImageAdapter = ProductImageAdapter(
            onDeleteClick = { imageUrl ->
                // Если это существующее изображение, добавляем в список на удаление
                if (imageUrl.startsWith("http")) {
                    imagesToDelete.add(imageUrl)
                }
                // Если это локальное изображение, удаляем из списка выбранных
                selectedImages.removeAll { it.toString() == imageUrl }
                updateImagesPreview()
            }
        )

        binding.rvProductImages.apply {
            layoutManager = LinearLayoutManager(this@EditProductActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = productImageAdapter
        }
    }

    private fun setupObservers() {
        // Наблюдение за данными товара
        viewModel.product.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    result.data?.let { product ->
                        currentProduct = product
                        updateUI(product)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_loading_product),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        })

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

        // Наблюдение за результатом обновления товара
        viewModel.updateProductResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.btnSaveProduct.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.btnSaveProduct.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, R.string.product_updated, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is Resource.Error -> {
                    binding.btnSaveProduct.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_updating_product),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        // Наблюдение за валидностью формы
        viewModel.productFormValid.observe(this, Observer { isValid ->
            binding.btnSaveProduct.isEnabled = isValid
        })
    }

    private fun setupListeners() {
        // Кнопка добавления изображений
        binding.btnAddImages.setOnClickListener {
            checkStoragePermission()
        }

        // Кнопка сохранения изменений
        binding.btnSaveProduct.setOnClickListener {
            saveProduct()
        }

        // Переключатель для скидки
        binding.switchDiscount.setOnCheckedChangeListener { _, isChecked ->
            binding.discountPriceLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            validateForm()
        }

        // Переключатель доступности товара
        binding.switchAvailability.setOnCheckedChangeListener { _, isChecked ->
            binding.availableQuantityLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            validateForm()
        }
    }

    private fun setupValidation() {
        // Валидация названия товара
        binding.etProductName.doOnTextChanged { text, _, _, _ ->
            binding.tilProductName.error = if (text.toString().length < 3) {
                getString(R.string.invalid_product_name)
            } else {
                null
            }
            validateForm()
        }

        // Валидация описания товара
        binding.etProductDescription.doOnTextChanged { text, _, _, _ ->
            binding.tilProductDescription.error = if (text.toString().length < 10) {
                getString(R.string.invalid_product_description)
            } else {
                null
            }
            validateForm()
        }

        // Валидация цены товара
        binding.etProductPrice.doOnTextChanged { text, _, _, _ ->
            binding.tilProductPrice.error = if (text.toString().isNotEmpty() && text.toString().toDoubleOrNull() ?: 0.0 <= 0) {
                getString(R.string.invalid_product_price)
            } else {
                null
            }
            validateForm()
        }

        // Валидация цены со скидкой
        binding.etDiscountPrice.doOnTextChanged { text, _, _, _ ->
            val regularPrice = binding.etProductPrice.text.toString().toDoubleOrNull() ?: 0.0
            val discountPrice = text.toString().toDoubleOrNull() ?: 0.0

            binding.tilDiscountPrice.error = if (binding.switchDiscount.isChecked &&
                (discountPrice <= 0 || discountPrice >= regularPrice)) {
                getString(R.string.invalid_discount_price)
            } else {
                null
            }
            validateForm()
        }

        // Валидация доступного количества
        binding.etAvailableQuantity.doOnTextChanged { text, _, _, _ ->
            binding.tilAvailableQuantity.error = if (binding.switchAvailability.isChecked &&
                (text.toString().toIntOrNull() ?: 0) <= 0) {
                getString(R.string.invalid_quantity)
            } else {
                null
            }
            validateForm()
        }

        // Валидация SKU
        binding.etSku.doOnTextChanged { text, _, _, _ ->
            binding.tilSku.error = if (text.toString().isEmpty()) {
                getString(R.string.invalid_sku)
            } else {
                null
            }
            validateForm()
        }
    }

    private fun setupCategorySpinner(categories: List<Category>) {
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categoryNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        // Установка слушателя для выбора категории
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryId = categories[position].id
                validateForm()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategoryId = null
                validateForm()
            }
        }

        // Если есть текущий товар, выбираем соответствующую категорию
        currentProduct?.let { product ->
            val categoryIndex = categories.indexOfFirst { it.id == product.categoryId }
            if (categoryIndex >= 0) {
                binding.spinnerCategory.setSelection(categoryIndex)
                selectedCategoryId = product.categoryId
            }
        }
    }

    private fun updateUI(product: Product) {
        // Заполнение полей формы данными товара
        binding.etProductName.setText(product.name)
        binding.etProductDescription.setText(product.description)
        binding.etProductPrice.setText(product.price.toString())
        binding.etSku.setText(product.sku)

        // Скидка
        if (product.discountPrice != null && product.discountPrice < product.price) {
            binding.switchDiscount.isChecked = true
            binding.etDiscountPrice.setText(product.discountPrice.toString())
            binding.discountPriceLayout.visibility = View.VISIBLE
        } else {
            binding.switchDiscount.isChecked = false
            binding.discountPriceLayout.visibility = View.GONE
        }

        // Наличие
        if (product.availableQuantity > 0) {
            binding.switchAvailability.isChecked = true
            binding.etAvailableQuantity.setText(product.availableQuantity.toString())
            binding.availableQuantityLayout.visibility = View.VISIBLE
        } else {
            binding.switchAvailability.isChecked = false
            binding.availableQuantityLayout.visibility = View.GONE
        }

        // Спецификации (особенности товара)
        val specifications = product.specifications.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        binding.etSpecifications.setText(specifications)

        // Обновление списка изображений
        productImageAdapter.submitList(product.images)

        // Включаем кнопку сохранения
        binding.btnSaveProduct.isEnabled = true
    }

    private fun validateForm() {
        val name = binding.etProductName.text.toString()
        val description = binding.etProductDescription.text.toString()
        val price = binding.etProductPrice.text.toString().toDoubleOrNull()
        val sku = binding.etSku.text.toString()
        val hasCategory = selectedCategoryId != null

        var isDiscountValid = true
        if (binding.switchDiscount.isChecked) {
            val discountPrice = binding.etDiscountPrice.text.toString().toDoubleOrNull()
            isDiscountValid = discountPrice != null && discountPrice > 0 && price != null && discountPrice < price
        }

        var isQuantityValid = true
        if (binding.switchAvailability.isChecked) {
            val quantity = binding.etAvailableQuantity.text.toString().toIntOrNull()
            isQuantityValid = quantity != null && quantity > 0
        }

        val isValid = name.length >= 3 &&
                description.length >= 10 &&
                price != null && price > 0 &&
                sku.isNotEmpty() &&
                hasCategory &&
                isDiscountValid &&
                isQuantityValid &&
                (selectedImages.isNotEmpty() || (currentProduct?.images?.isNotEmpty() == true && imagesToDelete.size < currentProduct?.images?.size ?: 0))

        viewModel.setProductFormValid(isValid)
    }

    private fun saveProduct() {
        try {
            val name = binding.etProductName.text.toString()
            val description = binding.etProductDescription.text.toString()
            val price = binding.etProductPrice.text.toString().toDouble()
            val sku = binding.etSku.text.toString()
            val categoryId = selectedCategoryId ?: return

            // Значения скидки и количества
            val discountPrice = if (binding.switchDiscount.isChecked) {
                binding.etDiscountPrice.text.toString().toDoubleOrNull()
            } else {
                null
            }

            val availableQuantity = if (binding.switchAvailability.isChecked) {
                binding.etAvailableQuantity.text.toString().toIntOrNull() ?: 0
            } else {
                0
            }

            // Парсинг спецификаций
            val specificationsText = binding.etSpecifications.text.toString()
            val specifications = mutableMapOf<String, String>()
            if (specificationsText.isNotEmpty()) {
                specificationsText.split("\n").forEach { line ->
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        specifications[parts[0].trim()] = parts[1].trim()
                    }
                }
            }

            // Преобразование URI в файлы
            val imageFiles = selectedImages.mapNotNull { uri ->
                try {
                    // Создаем временный файл
                    val tempFile = File.createTempFile("product", ".jpg", cacheDir)

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

            // Обновляем товар
            viewModel.updateProduct(
                productId = productId,
                name = name,
                description = description,
                price = price,
                discountPrice = discountPrice,
                availableQuantity = availableQuantity,
                categoryId = categoryId,
                sku = sku,
                specifications = specifications,
                imageFiles = imageFiles,
                imagesToDelete = imagesToDelete
            )
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
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        imagePickerLauncher.launch(intent)
    }

    private fun updateImagesPreview() {
        // Обновляем адаптер с текущими изображениями
        val existingImages = currentProduct?.images?.filter { !imagesToDelete.contains(it) } ?: emptyList()
        val newImageUris = selectedImages.map { it.toString() }

        val allImages = existingImages + newImageUris
        productImageAdapter.submitList(allImages)

        // Валидируем форму
        validateForm()
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
        AlertDialog.Builder(this)
            .setTitle(R.string.discard_changes)
            .setMessage(R.string.discard_changes_message)
            .setPositiveButton(R.string.yes) { _, _ -> super.onBackPressed() }
            .setNegativeButton(R.string.no, null)
            .show()
    }
}
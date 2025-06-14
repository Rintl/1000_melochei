package com.example.a1000_melochei.ui.customer.catalog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Product
import com.example.a1000_melochei.databinding.ActivityProductDetailBinding
import com.example.a1000_melochei.ui.customer.CustomerActivity
import com.example.a1000_melochei.ui.customer.catalog.adapter.ProductImageAdapter
import com.example.a1000_melochei.ui.customer.catalog.viewmodel.ProductViewModel
import com.example.a1000_melochei.util.CurrencyFormatter
import org.koin.androidx.viewmodel.ext.android.viewModel


/**
 * Активность с подробной информацией о товаре.
 * Отображает изображения, описание, характеристики, цену, наличие.
 * Позволяет добавить товар в корзину.
 */
class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val viewModel: ProductViewModel by viewModel()

    private lateinit var imageAdapter: ProductImageAdapter
    private var productId: String = ""
    private var currentProduct: Product? = null
    private var quantity: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем ID товара из переданных данных
        productId = intent.getStringExtra("PRODUCT_ID") ?: ""
        if (productId.isEmpty()) {
            Toast.makeText(this, R.string.error_invalid_product, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupImageSlider()
        setupQuantityControls()
        setupObservers()
        setupListeners()

        // Загружаем информацию о товаре
        viewModel.loadProduct(productId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
    }

    private fun setupImageSlider() {
        imageAdapter = ProductImageAdapter()
        binding.viewPager.adapter = imageAdapter

        // Настройка индикатора для ViewPager
        binding.dotsIndicator.setViewPager2(binding.viewPager)

        // Обработка изменения страницы во ViewPager
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Можно реагировать на изменение текущего изображения
            }
        })
    }

    private fun setupQuantityControls() {
        // Настройка контроля количества товара
        binding.btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateQuantityUI()
            }
        }

        binding.btnIncrease.setOnClickListener {
            // Проверяем, не превышает ли количество наличие
            currentProduct?.let { product ->
                if (quantity < product.availableQuantity) {
                    quantity++
                    updateQuantityUI()
                } else {
                    Toast.makeText(this, R.string.error_max_quantity, Toast.LENGTH_SHORT).show()
                }
            }
        }

        updateQuantityUI()
    }

    private fun updateQuantityUI() {
        binding.tvQuantity.text = quantity.toString()
    }

    private fun setupObservers() {
        // Наблюдение за данными товара
        viewModel.product.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                    showError(false)
                }
                is Resource.Success -> {
                    showLoading(false)
                    showError(false)

                    result.data?.let { product ->
                        currentProduct = product
                        updateProductUI(product)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(true, result.message)
                }
            }
        })

        // Наблюдение за результатом добавления в корзину
        viewModel.addToCartResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.btnAddToCart.isEnabled = false
                    binding.addToCartProgressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.btnAddToCart.isEnabled = true
                    binding.addToCartProgressBar.visibility = View.GONE

                    showAddToCartAnimation()

                    Snackbar.make(
                        binding.root,
                        getString(R.string.product_added_to_cart),
                        Snackbar.LENGTH_LONG
                    ).setAction(R.string.go_to_cart) {
                        navigateToCart()
                    }.show()
                }
                is Resource.Error -> {
                    binding.btnAddToCart.isEnabled = true
                    binding.addToCartProgressBar.visibility = View.GONE

                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_adding_to_cart),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun setupListeners() {
        // Кнопка добавления в корзину
        binding.btnAddToCart.setOnClickListener {
            currentProduct?.let { product ->
                viewModel.addToCart(product, quantity)
            }
        }

        // Кнопка "Купить сейчас"
        binding.btnBuyNow.setOnClickListener {
            currentProduct?.let { product ->
                viewModel.addToCart(product, quantity)
                navigateToCart()
            }
        }

        // Клик по вкладкам информации о товаре
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                updateTabContent(tab?.position ?: 0)
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // Ничего не делаем
            }

            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // Ничего не делаем
            }
        })
    }

    private fun updateProductUI(product: Product) {
        // Обновляем заголовок
        supportActionBar?.title = product.name
        binding.tvProductName.text = product.name

        // Обновляем изображения
        imageAdapter.submitList(product.images)
        binding.dotsIndicator.isVisible = product.images.size > 1

        // Обновляем цены
        if (product.discountPrice != null && product.discountPrice < product.price) {
            binding.tvPrice.text = CurrencyFormatter.format(product.discountPrice)
            binding.tvOriginalPrice.isVisible = true
            binding.tvOriginalPrice.text = CurrencyFormatter.format(product.price)
            binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.tvPrice.text = CurrencyFormatter.format(product.price)
            binding.tvOriginalPrice.isVisible = false
        }

        // Обновляем статус наличия
        if (product.availableQuantity > 0) {
            binding.tvAvailability.text = getString(R.string.in_stock)
            binding.tvAvailability.setTextColor(ContextCompat.getColor(this, R.color.available))
            binding.btnAddToCart.isEnabled = true
            binding.btnBuyNow.isEnabled = true
        } else {
            binding.tvAvailability.text = getString(R.string.out_of_stock)
            binding.tvAvailability.setTextColor(ContextCompat.getColor(this, R.color.unavailable))
            binding.btnAddToCart.isEnabled = false
            binding.btnBuyNow.isEnabled = false
        }

        // Устанавливаем выбранную вкладку описания товара
        binding.tabLayout.getTabAt(0)?.select()
        updateTabContent(0)

        // Показываем лейбл распродажи, если товар со скидкой
        binding.tvSale.isVisible = product.discountPrice != null && product.discountPrice < product.price

        // Показываем рейтинг товара
        binding.ratingBar.rating = product.rating
        binding.tvRatingCount.text = getString(R.string.rating_count, product.reviewCount)
        binding.ratingLayout.isVisible = product.reviewCount > 0
    }

    private fun updateTabContent(position: Int) {
        binding.tvDescription.isVisible = position == 0
        binding.tvSpecifications.isVisible = position == 1
        binding.tvReviews.isVisible = position == 2

        currentProduct?.let { product ->
            when (position) {
                0 -> binding.tvDescription.text = product.description
                1 -> {
                    val specs = StringBuilder()
                    product.specifications.forEach { (key, value) ->
                        specs.append("• $key: $value\n")
                    }
                    binding.tvSpecifications.text = specs.toString()
                }
                2 -> {
                    if (product.reviews.isEmpty()) {
                        binding.tvReviews.text = getString(R.string.no_reviews)
                    } else {
                        val reviews = StringBuilder()
                        product.reviews.forEach { review ->
                            reviews.append("★ ${review.rating}/5 - ${review.authorName}\n")
                            reviews.append("${review.text}\n\n")
                        }
                        binding.tvReviews.text = reviews.toString()
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.contentLayout.isVisible = !isLoading && !binding.errorLayout.isVisible
    }

    private fun showError(isError: Boolean, message: String? = null) {
        binding.errorLayout.isVisible = isError
        binding.errorMessage.text = message ?: getString(R.string.error_loading_product)
        binding.contentLayout.isVisible = !isError && !binding.progressBar.isVisible

        binding.btnRetry.setOnClickListener {
            viewModel.loadProduct(productId)
        }
    }

    private fun showAddToCartAnimation() {
        binding.ivAddToCartAnimation.alpha = 0.8f
        binding.ivAddToCartAnimation.visibility = View.VISIBLE
        binding.ivAddToCartAnimation.scaleX = 1.0f
        binding.ivAddToCartAnimation.scaleY = 1.0f

        binding.ivAddToCartAnimation.animate()
            .alpha(0f)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .setDuration(500)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.ivAddToCartAnimation.visibility = View.GONE
                }
            })
            .start()
    }

    private fun navigateToCart() {
        val intent = Intent(this, CustomerActivity::class.java).apply {
            putExtra("NAVIGATE_TO_CART", true)
        }
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.product_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_share -> {
                shareProduct()
                true
            }
            R.id.action_favorite -> {
                toggleFavorite()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareProduct() {
        currentProduct?.let { product ->
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT,
                    "${product.name}\n${product.description}\nЦена: ${CurrencyFormatter.format(product.price)}\n\nПоделено из приложения 1000 мелочей")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_product)))
        }
    }

    private fun toggleFavorite() {
        currentProduct?.let { product ->
            viewModel.toggleFavorite(product)

            val message = if (product.isFavorite) {
                getString(R.string.removed_from_favorites)
            } else {
                getString(R.string.added_to_favorites)
            }

            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
    }
}
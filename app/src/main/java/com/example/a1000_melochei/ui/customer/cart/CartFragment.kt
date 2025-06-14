package com.example.a1000_melochei.ui.customer.cart

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.CartItem
import com.example.a1000_melochei.databinding.FragmentCartBinding
import com.example.a1000_melochei.ui.customer.cart.adapter.CartItemAdapter
import com.example.a1000_melochei.ui.customer.cart.viewmodel.CartViewModel
import com.example.a1000_melochei.ui.customer.catalog.ProductDetailActivity
import com.example.a1000_melochei.util.CurrencyFormatter
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Фрагмент корзины покупок.
 * Отображает список товаров в корзине, позволяет изменять их количество,
 * удалять товары и переходить к оформлению заказа.
 */
class CartFragment : Fragment(), CartItemAdapter.CartItemListener {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CartViewModel by viewModel()
    private lateinit var cartItemAdapter: CartItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Загружаем содержимое корзины
        viewModel.loadCart()
    }

    private fun setupRecyclerView() {
        cartItemAdapter = CartItemAdapter(this)

        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartItemAdapter
        }
    }

    private fun setupObservers() {
        // Наблюдение за содержимым корзины
        viewModel.cartItems.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                    showError(false)
                }
                is Resource.Success -> {
                    showLoading(false)
                    showError(false)

                    result.data?.let { cartItems ->
                        if (cartItems.isEmpty()) {
                            showEmptyState(true)
                        } else {
                            showEmptyState(false)
                            cartItemAdapter.submitList(cartItems)
                        }
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    showEmptyState(false)
                    showError(true, result.message)
                }
            }
        })

        // Наблюдение за общей суммой корзины
        viewModel.cartTotal.observe(viewLifecycleOwner, Observer { cartTotal ->
            binding.tvSubtotal.text = CurrencyFormatter.format(cartTotal.subtotal)
            binding.tvDelivery.text = if (cartTotal.deliveryFee > 0) {
                CurrencyFormatter.format(cartTotal.deliveryFee)
            } else {
                getString(R.string.free)
            }
            binding.tvTotal.text = CurrencyFormatter.format(cartTotal.total)

            // Проверка минимальной суммы заказа для доставки
            val minOrderAmountMet = cartTotal.subtotal >= viewModel.getMinOrderAmount()
            binding.btnCheckout.isEnabled = minOrderAmountMet

            if (!minOrderAmountMet && cartTotal.subtotal > 0) {
                val minAmount = CurrencyFormatter.format(viewModel.getMinOrderAmount())
                binding.tvMinOrderWarning.text = getString(R.string.min_order_amount_warning, minAmount)
                binding.tvMinOrderWarning.visibility = View.VISIBLE
            } else {
                binding.tvMinOrderWarning.visibility = View.GONE
            }
        })

        // Наблюдение за результатом обновления количества
        viewModel.updateQuantityResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: getString(R.string.error_updating_quantity),
                        Toast.LENGTH_SHORT
                    ).show()
                    // Перезагружаем корзину, чтобы показать актуальные данные
                    viewModel.loadCart()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })

        // Наблюдение за результатом удаления товара
        viewModel.removeItemResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: getString(R.string.error_removing_item),
                        Toast.LENGTH_SHORT
                    ).show()
                    // Перезагружаем корзину для актуализации
                    viewModel.loadCart()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })
    }

    private fun setupListeners() {
        // Кнопка очистки корзины
        binding.btnClearCart.setOnClickListener {
            showClearCartConfirmation()
        }

        // Кнопка перехода к оформлению заказа
        binding.btnCheckout.setOnClickListener {
            if (binding.btnCheckout.isEnabled) {
                navigateToCheckout()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.min_order_amount_not_met),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Кнопка обновления
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadCart()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Кнопка повторной попытки загрузки при ошибке
        binding.btnRetry.setOnClickListener {
            viewModel.loadCart()
        }

        // Кнопка перехода в каталог при пустой корзине
        binding.btnGoShopping.setOnClickListener {
            navigateToCatalog()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.rvCartItems.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
        binding.cartSummaryLayout.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
        binding.btnClearCart.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showError(isError: Boolean, message: String? = null) {
        binding.errorLayout.isVisible = isError
        binding.errorMessage.text = message ?: getString(R.string.error_loading_cart)
        binding.rvCartItems.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
        binding.cartSummaryLayout.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
        binding.btnClearCart.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyLayout.isVisible = isEmpty
        binding.rvCartItems.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
        binding.cartSummaryLayout.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
        binding.btnClearCart.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
    }

    private fun showClearCartConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_cart_title)
            .setMessage(R.string.clear_cart_message)
            .setPositiveButton(R.string.clear) { _, _ ->
                viewModel.clearCart()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun navigateToCheckout() {
        val intent = Intent(requireContext(), CheckoutActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCatalog() {
        // Переключаемся на таб каталога в нижней навигации
        requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.bottom_nav_view
        )?.selectedItemId = R.id.navigation_catalog
    }

    override fun onItemClick(cartItem: CartItem) {
        // Переход на страницу товара
        val intent = Intent(requireContext(), ProductDetailActivity::class.java).apply {
            putExtra("PRODUCT_ID", cartItem.productId)
        }
        startActivity(intent)
    }

    override fun onQuantityChanged(cartItem: CartItem, newQuantity: Int) {
        // Обновление количества товара
        viewModel.updateCartItemQuantity(cartItem.id, newQuantity)
    }

    override fun onRemoveClick(cartItem: CartItem) {
        // Удаление товара из корзины
        viewModel.removeCartItem(cartItem.id)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
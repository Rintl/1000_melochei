package com.yourstore.app.ui.admin.analytics

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.databinding.FragmentAnalyticsBinding
import com.yourstore.app.ui.admin.AdminActivity
import com.yourstore.app.ui.admin.analytics.adapter.TopProductsAdapter
import com.yourstore.app.ui.admin.analytics.viewmodel.AnalyticsViewModel
import com.yourstore.app.util.CurrencyFormatter
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Фрагмент для отображения аналитики и статистики магазина
 */
class AnalyticsFragment : Fragment(), AdminActivity.Refreshable {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyticsViewModel by viewModel()
    private lateinit var topProductsAdapter: TopProductsAdapter

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private var startDate: Date = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1)
    }.time
    private var endDate: Date = Calendar.getInstance().time

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        setupRecyclerView()
        setupListeners()

        // Загружаем данные за последний месяц по умолчанию
        loadData()
    }

    private fun setupUI() {
        // Настраиваем визуальные элементы
        updateDateRangeText()

        // Настраиваем выпадающий список с периодами
        val periods = arrayOf(
            getString(R.string.custom_period),
            getString(R.string.today),
            getString(R.string.last_7_days),
            getString(R.string.last_30_days),
            getString(R.string.last_year)
        )

        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            periods
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPeriod.adapter = adapter
        }

        // Выбираем "Последние 30 дней" по умолчанию
        binding.spinnerPeriod.setSelection(3)
    }

    private fun setupRecyclerView() {
        topProductsAdapter = TopProductsAdapter()
        binding.rvTopProducts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = topProductsAdapter
        }
    }

    private fun setupObservers() {
        // Наблюдение за общими показателями продаж
        viewModel.currentPeriodSummary.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.salesSummaryProgressBar.isVisible = true
                    binding.salesSummaryLayout.isVisible = false
                }
                is Resource.Success -> {
                    binding.salesSummaryProgressBar.isVisible = false
                    binding.salesSummaryLayout.isVisible = true

                    result.data?.let { summary ->
                        binding.tvTotalSalesValue.text = CurrencyFormatter.format(summary.totalSales)
                        binding.tvTotalOrdersValue.text = summary.totalOrders.toString()
                        binding.tvAverageOrderValue.text = CurrencyFormatter.format(summary.averageOrderValue)

                        // Отображаем изменение в сравнении с предыдущим периодом
                        if (summary.comparisonPercentage != 0.0) {
                            val percentText = String.format("%.1f%%", summary.comparisonPercentage)
                            binding.tvSalesComparisonValue.text = percentText

                            if (summary.comparisonPercentage > 0) {
                                binding.tvSalesComparisonValue.setTextColor(
                                    ContextCompat.getColor(requireContext(), R.color.green)
                                )
                                binding.ivSalesComparisonIcon.setImageResource(R.drawable.ic_arrow_up)
                                binding.ivSalesComparisonIcon.setColorFilter(
                                    ContextCompat.getColor(requireContext(), R.color.green)
                                )
                            } else {
                                binding.tvSalesComparisonValue.setTextColor(
                                    ContextCompat.getColor(requireContext(), R.color.red)
                                )
                                binding.ivSalesComparisonIcon.setImageResource(R.drawable.ic_arrow_down)
                                binding.ivSalesComparisonIcon.setColorFilter(
                                    ContextCompat.getColor(requireContext(), R.color.red)
                                )
                            }

                            binding.salesComparisonLayout.isVisible = true
                        } else {
                            binding.salesComparisonLayout.isVisible = false
                        }
                    }
                }
                is Resource.Error -> {
                    binding.salesSummaryProgressBar.isVisible = false
                    binding.salesSummaryLayout.isVisible = false
                    showErrorMessage(result.message ?: getString(R.string.error_loading_sales_data))
                }
            }
        })

        // Наблюдение за данными о продажах по дням
        viewModel.dailySalesData.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.salesChartProgressBar.isVisible = true
                    binding.salesChart.isVisible = false
                }
                is Resource.Success -> {
                    binding.salesChartProgressBar.isVisible = false
                    binding.salesChart.isVisible = true

                    result.data?.let { salesData ->
                        updateSalesChart(salesData)
                    }
                }
                is Resource.Error -> {
                    binding.salesChartProgressBar.isVisible = false
                    binding.salesChart.isVisible = false
                    showErrorMessage(result.message ?: getString(R.string.error_loading_sales_chart))
                }
            }
        })

        // Наблюдение за данными о продажах по категориям
        viewModel.categorySalesData.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.categoriesChartProgressBar.isVisible = true
                    binding.categoriesChart.isVisible = false
                }
                is Resource.Success -> {
                    binding.categoriesChartProgressBar.isVisible = false
                    binding.categoriesChart.isVisible = true

                    result.data?.let { categoryData ->
                        updateCategoriesChart(categoryData)
                    }
                }
                is Resource.Error -> {
                    binding.categoriesChartProgressBar.isVisible = false
                    binding.categoriesChart.isVisible = false
                    showErrorMessage(result.message ?: getString(R.string.error_loading_categories_chart))
                }
            }
        })

        // Наблюдение за данными о самых продаваемых товарах
        viewModel.topProducts.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.topProductsProgressBar.isVisible = true
                    binding.rvTopProducts.isVisible = false
                    binding.tvNoTopProducts.isVisible = false
                }
                is Resource.Success -> {
                    binding.topProductsProgressBar.isVisible = false

                    result.data?.let { products ->
                        if (products.isEmpty()) {
                            binding.rvTopProducts.isVisible = false
                            binding.tvNoTopProducts.isVisible = true
                        } else {
                            binding.rvTopProducts.isVisible = true
                            binding.tvNoTopProducts.isVisible = false
                            topProductsAdapter.submitList(products)
                        }
                    }
                }
                is Resource.Error -> {
                    binding.topProductsProgressBar.isVisible = false
                    binding.rvTopProducts.isVisible = false
                    binding.tvNoTopProducts.isVisible = true
                    binding.tvNoTopProducts.text = result.message ?: getString(R.string.error_loading_top_products)
                }
            }
        })
    }

    private fun setupListeners() {
        // Выбор периода из выпадающего списка
        binding.spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        // Пользовательский период - показываем элементы выбора дат
                        binding.dateSelectionLayout.isVisible = true
                    }
                    1 -> {
                        // Сегодня
                        binding.dateSelectionLayout.isVisible = false
                        viewModel.loadDataForPredefinedPeriod(AnalyticsViewModel.PeriodType.TODAY)
                    }
                    2 -> {
                        // Последние 7 дней
                        binding.dateSelectionLayout.isVisible = false
                        viewModel.loadDataForPredefinedPeriod(AnalyticsViewModel.PeriodType.WEEK)
                    }
                    3 -> {
                        // Последние 30 дней
                        binding.dateSelectionLayout.isVisible = false
                        viewModel.loadDataForPredefinedPeriod(AnalyticsViewModel.PeriodType.MONTH)
                    }
                    4 -> {
                        // Последний год
                        binding.dateSelectionLayout.isVisible = false
                        viewModel.loadDataForPredefinedPeriod(AnalyticsViewModel.PeriodType.YEAR)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Ничего не делаем
            }
        }

        // Кнопки выбора дат
        binding.btnSelectStartDate.setOnClickListener {
            showDatePicker(true)
        }

        binding.btnSelectEndDate.setOnClickListener {
            showDatePicker(false)
        }

        // Кнопка применения фильтра по датам
        binding.btnApplyDateFilter.setOnClickListener {
            loadData()
        }

        // Кнопка обновления данных
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadData()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()

        // Устанавливаем начальную дату в календаре
        val initialDate = if (isStartDate) startDate else endDate
        calendar.time = initialDate

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)

                if (isStartDate) {
                    startDate = calendar.time
                    binding.tvStartDate.text = dateFormat.format(startDate)
                } else {
                    endDate = calendar.time
                    binding.tvEndDate.text = dateFormat.format(endDate)
                }

                // Проверяем корректность выбранного периода
                if (startDate.after(endDate)) {
                    if (isStartDate) {
                        endDate = startDate
                        binding.tvEndDate.text = dateFormat.format(endDate)
                    } else {
                        startDate = endDate
                        binding.tvStartDate.text = dateFormat.format(startDate)
                    }
                }
            },
            year,
            month,
            day
        ).show()
    }

    private fun updateDateRangeText() {
        binding.tvStartDate.text = dateFormat.format(startDate)
        binding.tvEndDate.text = dateFormat.format(endDate)
    }

    private fun loadData() {
        viewModel.loadAnalyticsData(startDate, endDate)
    }

    private fun updateSalesChart(salesData: List<AnalyticsViewModel.DailySalesData>) {
        // Настраиваем диаграмму продаж по дням
        binding.salesChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)

            val barEntries = ArrayList<BarEntry>()
            val xLabels = ArrayList<String>()

            salesData.forEachIndexed { index, data ->
                barEntries.add(BarEntry(index.toFloat(), data.salesAmount.toFloat()))
                xLabels.add(data.date)
            }

            val dataSet = BarDataSet(barEntries, getString(R.string.sales_amount))
            dataSet.color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)

            val data = BarData(dataSet)
            data.setValueTextSize(10f)
            setData(data)

            // Настройка осей
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.labelRotationAngle = -45f
            xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)

            axisRight.isEnabled = false

            // Анимация и обновление
            animateY(1000)
            invalidate()
        }
    }

    private fun updateCategoriesChart(categoryData: List<AnalyticsViewModel.CategorySalesData>) {
        // Настраиваем круговую диаграмму категорий
        binding.categoriesChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(android.R.color.transparent)

            // Создаем данные для диаграммы
            val pieEntries = ArrayList<PieEntry>()
            val colors = ArrayList<Int>()

            // Используем только топ-5 категорий, остальные объединяем в "Другие"
            val topCategories = categoryData.take(5)
            var otherTotal = 0.0

            if (categoryData.size > 5) {
                categoryData.subList(5, categoryData.size).forEach {
                    otherTotal += it.salesAmount
                }
            }

            // Добавляем топ-5 категорий
            topCategories.forEach { category ->
                pieEntries.add(PieEntry(category.salesAmount.toFloat(), category.categoryName))
                colors.add(getRandomColor())
            }

            // Добавляем "Другие" если есть
            if (otherTotal > 0) {
                pieEntries.add(PieEntry(otherTotal.toFloat(), getString(R.string.other_categories)))
                colors.add(ContextCompat.getColor(requireContext(), R.color.gray))
            }

            val dataSet = PieDataSet(pieEntries, "")
            dataSet.colors = colors
            dataSet.valueTextSize = 12f

            val data = PieData(dataSet)
            setData(data)

            // Анимация и обновление
            animateY(1000)
            invalidate()
        }
    }

    private fun getRandomColor(): Int {
        // Генерируем яркие цвета для диаграммы
        val colors = intArrayOf(
            R.color.colorPrimary,
            R.color.colorAccent,
            R.color.green,
            R.color.blue,
            R.color.orange,
            R.color.purple,
            R.color.cyan,
            R.color.yellow
        )

        return ContextCompat.getColor(
            requireContext(),
            colors[(Math.random() * colors.size).toInt()]
        )
    }

    private fun showErrorMessage(message: String) {
        // Показываем сообщение об ошибке
        binding.tvErrorMessage.text = message
        binding.tvErrorMessage.isVisible = true
    }

    override fun refresh() {
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
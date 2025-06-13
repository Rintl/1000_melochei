package com.example.a1000_melochei.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.a1000_melochei.R
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Расширения для Context
 */

/**
 * Показывает короткий Toast с сообщением
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Показывает длинный Toast с сообщением
 */
fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

/**
 * Показывает диалог с подтверждением
 */
fun Context.showConfirmDialog(
    title: String,
    message: String,
    positiveButtonText: String = getString(R.string.yes),
    negativeButtonText: String = getString(R.string.no),
    onPositiveClick: () -> Unit,
    onNegativeClick: (() -> Unit)? = null
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positiveButtonText) { _, _ -> onPositiveClick() }
        .setNegativeButton(negativeButtonText) { _, _ -> onNegativeClick?.invoke() }
        .show()
}

/**
 * Показывает простой информационный диалог
 */
fun Context.showInfoDialog(
    title: String,
    message: String,
    buttonText: String = getString(R.string.ok),
    onButtonClick: (() -> Unit)? = null
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(buttonText) { _, _ -> onButtonClick?.invoke() }
        .show()
}

/**
 * Получает абсолютный путь к временному файлу в директории приложения
 */
fun Context.getTempFilePath(prefix: String = Constants.TEMP_FILE_PREFIX, extension: String = ".jpg"): String {
    val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "${prefix}${timeStamp}${extension}"
    return File(storageDir, fileName).absolutePath
}

/**
 * Создает общедоступный Uri для файла с использованием FileProvider
 */
fun Context.getUriForFile(file: File): Uri {
    return FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        file
    )
}

/**
 * Проверяет, доступен ли интернет
 */
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        networkInfo?.isConnected == true
    }
}

/**
 * Открывает номер телефона в приложении для звонков
 */
fun Context.openPhoneDialer(phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        startActivity(intent)
    } catch (e: Exception) {
        showToast("Не удалось открыть приложение для звонков")
    }
}

/**
 * Открывает email в почтовом приложении
 */
fun Context.openEmailClient(email: String, subject: String = "", body: String = "") {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startActivity(Intent.createChooser(intent, "Выберите почтовое приложение"))
    } catch (e: Exception) {
        showToast("Не удалось открыть почтовое приложение")
    }
}

/**
 * Расширения для Activity
 */

/**
 * Скрывает клавиатуру
 */
fun Activity.hideKeyboard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val currentFocus = currentFocus ?: View(this)
    imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
}

/**
 * Показывает клавиатуру для определенного View
 */
fun Activity.showKeyboard(view: View) {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    view.requestFocus()
    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * Устанавливает прозрачность статус-бара
 */
fun AppCompatActivity.setTransparentStatusBar() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
    }
}

/**
 * Устанавливает цвет статус-бара
 */
fun AppCompatActivity.setStatusBarColor(colorRes: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.statusBarColor = ContextCompat.getColor(this, colorRes)
    }
}

/**
 * Расширения для Fragment
 */

/**
 * Показывает короткий Toast с сообщением в контексте фрагмента
 */
fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    context?.showToast(message, duration)
}

/**
 * Показывает Snackbar с сообщением
 */
fun Fragment.showSnackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionText: String? = null,
    action: ((View) -> Unit)? = null
) {
    view?.let { view ->
        val snackbar = Snackbar.make(view, message, duration)
        if (actionText != null && action != null) {
            snackbar.setAction(actionText, action)
        }
        snackbar.show()
    }
}

/**
 * Скрывает клавиатуру в контексте фрагмента
 */
fun Fragment.hideKeyboard() {
    activity?.hideKeyboard()
}

/**
 * Показывает диалог подтверждения в контексте фрагмента
 */
fun Fragment.showConfirmDialog(
    title: String,
    message: String,
    positiveButtonText: String = getString(R.string.yes),
    negativeButtonText: String = getString(R.string.no),
    onPositiveClick: () -> Unit,
    onNegativeClick: (() -> Unit)? = null
) {
    context?.showConfirmDialog(title, message, positiveButtonText, negativeButtonText, onPositiveClick, onNegativeClick)
}

/**
 * Расширения для ImageView
 */

/**
 * Загружает изображение с помощью Glide
 */
fun ImageView.loadImage(
    imageUrl: String?,
    placeholder: Int = R.drawable.ic_placeholder,
    error: Int = R.drawable.ic_error
) {
    Glide.with(context)
        .load(imageUrl)
        .placeholder(placeholder)
        .error(error)
        .into(this)
}

/**
 * Загружает изображение с закругленными углами
 */
fun ImageView.loadRoundedImage(
    imageUrl: String?,
    cornerRadius: Int = 8,
    placeholder: Int = R.drawable.ic_placeholder,
    error: Int = R.drawable.ic_error
) {
    Glide.with(context)
        .load(imageUrl)
        .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(cornerRadius)))
        .placeholder(placeholder)
        .error(error)
        .into(this)
}

/**
 * Загружает круглое изображение
 */
fun ImageView.loadCircularImage(
    imageUrl: String?,
    placeholder: Int = R.drawable.ic_placeholder,
    error: Int = R.drawable.ic_error
) {
    Glide.with(context)
        .load(imageUrl)
        .apply(RequestOptions.circleCropTransform())
        .placeholder(placeholder)
        .error(error)
        .into(this)
}

/**
 * Расширения для String
 */

/**
 * Проверяет, является ли строка валидным email
 */
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * Проверяет, является ли строка валидным номером телефона
 */
fun String.isValidPhone(): Boolean {
    val cleanPhone = this.replace(Regex("[^+\\d]"), "")
    return cleanPhone.length >= 10 && cleanPhone.length <= 15
}

/**
 * Форматирует строку как цену в тенге
 */
fun String.formatAsPrice(): String {
    return try {
        val number = this.toDoubleOrNull() ?: 0.0
        "${NumberFormat.getNumberInstance(Locale("ru", "KZ")).format(number.toInt())} ₸"
    } catch (e: Exception) {
        "$this ₸"
    }
}

/**
 * Обрезает строку до указанной длины с добавлением многоточия
 */
fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) {
        this
    } else {
        substring(0, maxLength - 3) + "..."
    }
}

/**
 * Капитализирует первую букву строки
 */
fun String.capitalize(): String {
    return if (isEmpty()) {
        this
    } else {
        this[0].uppercase() + substring(1).lowercase()
    }
}

/**
 * Расширения для Double
 */

/**
 * Форматирует число как цену в тенге
 */
fun Double.formatAsPrice(): String {
    return "${NumberFormat.getNumberInstance(Locale("ru", "KZ")).format(this.toInt())} ₸"
}

/**
 * Форматирует число с указанием количества знаков после запятой
 */
fun Double.formatWithDecimals(decimals: Int = 2): String {
    return String.format(Locale.getDefault(), "%.${decimals}f", this)
}

/**
 * Расширения для Long (timestamp)
 */

/**
 * Форматирует timestamp как дату
 */
fun Long.formatAsDate(pattern: String = "dd.MM.yyyy"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

/**
 * Форматирует timestamp как дату и время
 */
fun Long.formatAsDateTime(pattern: String = "dd.MM.yyyy HH:mm"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

/**
 * Форматирует timestamp как время
 */
fun Long.formatAsTime(pattern: String = "HH:mm"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

/**
 * Возвращает разницу во времени в читаемом формате
 */
fun Long.getTimeAgo(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60_000 -> "только что"
        diff < 3600_000 -> "${diff / 60_000} мин назад"
        diff < 86400_000 -> "${diff / 3600_000} ч назад"
        diff < 604800_000 -> "${diff / 86400_000} дн назад"
        else -> formatAsDate()
    }
}

/**
 * Расширения для View
 */

/**
 * Делает View видимым
 */
fun View.visible() {
    visibility = View.VISIBLE
}

/**
 * Делает View невидимым (занимает место)
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * Скрывает View (не занимает место)
 */
fun View.gone() {
    visibility = View.GONE
}

/**
 * Переключает видимость View
 */
fun View.toggleVisibility() {
    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

/**
 * Устанавливает видимость View в зависимости от условия
 */
fun View.visibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/**
 * Расширения для Intent
 */

/**
 * Безопасно запускает Intent
 */
fun Context.safeStartActivity(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: Exception) {
        showToast("Не удалось открыть приложение")
    }
}
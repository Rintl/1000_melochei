package com.yourstore.app.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.yourstore.app.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Файл с расширениями Kotlin (extension functions) для различных классов.
 * Эти расширения упрощают часто используемые операции и повышают читаемость кода.
 */

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
 * Получает цвет из ресурсов с учетом версии Android
 */
fun Context.getColorCompat(colorResId: Int): Int {
    return ContextCompat.getColor(this, colorResId)
}

/**
 * Преобразует значение из dp в пиксели
 */
fun Context.dpToPx(dp: Float): Int {
    return (dp * (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
}

/**
 * Преобразует значение из пикселей в dp
 */
fun Context.pxToDp(px: Int): Float {
    return px.toFloat() / (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

/**
 * Создает диалог подтверждения с заданным заголовком и сообщением
 */
fun Context.showConfirmationDialog(
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
 * Показывает клавиатуру
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
    view?.let {
        val snackbar = Snackbar.make(it, message, duration)
        if (actionText != null && action != null) {
            snackbar.setAction(actionText, action)
        }
        snackbar.show()
    }
}

/**
 * Расширения для View
 */

/**
 * Делает view видимой
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * Скрывает view (оставляет место)
 */
fun View.hide() {
    visibility = View.INVISIBLE
}

/**
 * Скрывает view (не оставляет место)
 */
fun View.gone() {
    visibility = View.GONE
}

/**
 * Переключает видимость view
 */
fun View.toggleVisibility() {
    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

/**
 * Показывает Snackbar для данной View
 */
fun View.showSnackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionText: String? = null,
    action: ((View) -> Unit)? = null
) {
    val snackbar = Snackbar.make(this, message, duration)
    if (actionText != null && action != null) {
        snackbar.setAction(actionText, action)
    }
    snackbar.show()
}

/**
 * Устанавливает видимость View в зависимости от условия
 */
fun View.setVisibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/**
 * Устанавливает состояние enabled для View и всех дочерних элементов
 */
fun View.setEnabledRecursively(enabled: Boolean) {
    isEnabled = enabled
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).setEnabledRecursively(enabled)
        }
    }
}

/**
 * Расширения для EditText
 */

/**
 * Устанавливает слушатель изменения текста с упрощенным интерфейсом
 */
fun EditText.onTextChanged(listener: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            listener(s.toString())
        }
    })
}

/**
 * Устанавливает курсор в конец текста
 */
fun EditText.placeCursorToEnd() {
    this.setSelection(this.text.length)
}

/**
 * Расширения для TextInputLayout
 */

/**
 * Проверяет валидность ввода и устанавливает сообщение об ошибке
 */
fun TextInputLayout.validate(validator: () -> Boolean, errorMessage: String): Boolean {
    val isValid = validator()
    error = if (isValid) null else errorMessage
    return isValid
}

/**
 * Расширения для ImageView
 */

/**
 * Загружает изображение из URL с использованием Glide
 */
fun ImageView.loadFromUrl(
    url: String,
    placeholder: Int = R.drawable.placeholder_image,
    error: Int = R.drawable.placeholder_image
) {
    Glide.with(this.context)
        .load(url)
        .placeholder(placeholder)
        .error(error)
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(this)
}

/**
 * Загружает круглое изображение из URL с использованием Glide
 */
fun ImageView.loadCircleImageFromUrl(
    url: String,
    placeholder: Int = R.drawable.default_avatar,
    error: Int = R.drawable.default_avatar
) {
    Glide.with(this.context)
        .load(url)
        .placeholder(placeholder)
        .error(error)
        .apply(RequestOptions.circleCropTransform())
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(this)
}

/**
 * Расширения для Uri
 */

/**
 * Копирует Uri во временный файл
 */
fun Uri.copyToFile(context: Context): File? {
    var inputStream: InputStream? = null
    var outputStream: FileOutputStream? = null

    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "temp_${timeStamp}.jpg"
        val outputFile = File(context.cacheDir, fileName)

        inputStream = context.contentResolver.openInputStream(this)
        outputStream = FileOutputStream(outputFile)

        val buffer = ByteArray(4 * 1024) // 4KB buffer
        var read: Int

        while (inputStream?.read(buffer).also { read = it ?: -1 } != -1) {
            outputStream.write(buffer, 0, read)
        }

        outputStream.flush()
        return outputFile
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    } finally {
        try {
            inputStream?.close()
            outputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

/**
 * Получает Bitmap из Uri
 */
fun Uri.toBitmap(context: Context): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(this)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

/**
 * Расширения для String
 */

/**
 * Форматирует строку как номер телефона
 */
fun String.formatAsPhoneNumber(): String {
    if (this.isEmpty()) return this

    val digitsOnly = this.replace(Regex("[^\\d]"), "")
    if (digitsOnly.length < 10) return this

    val countryCode = if (digitsOnly.length > 10) digitsOnly.substring(0, digitsOnly.length - 10) else ""
    val areaCode = digitsOnly.substring(digitsOnly.length - 10, digitsOnly.length - 7)
    val firstPart = digitsOnly.substring(digitsOnly.length - 7, digitsOnly.length - 4)
    val secondPart = digitsOnly.substring(digitsOnly.length - 4)

    return if (countryCode.isNotEmpty()) {
        "+$countryCode ($areaCode) $firstPart-$secondPart"
    } else {
        "($areaCode) $firstPart-$secondPart"
    }
}

/**
 * Возвращает первые N символов строки, добавляя многоточие, если строка длиннее
 */
fun String.truncate(length: Int, ellipsis: String = "..."): String {
    return if (this.length > length) {
        this.substring(0, length - ellipsis.length) + ellipsis
    } else {
        this
    }
}

/**
 * Расширения для Number
 */

/**
 * Форматирует число как валюту
 */
fun Number.formatAsCurrency(
    currency: String = "₸",
    showDecimals: Boolean = true
): String {
    val format = NumberFormat.getNumberInstance(Locale("ru", "KZ")) as DecimalFormat

    if (showDecimals) {
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2
    } else {
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 0
    }

    return "${format.format(this)} $currency"
}

/**
 * Расширения для LiveData
 */

/**
 * Наблюдает за LiveData один раз (сразу удаляет Observer после первого вызова)
 */
fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T) {
            observer.onChanged(t)
            removeObserver(this)
        }
    })
}
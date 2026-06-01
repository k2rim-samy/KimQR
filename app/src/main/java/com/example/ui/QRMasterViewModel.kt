package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.QRCodeEntity
import com.example.data.database.QRCodeRepository
import com.example.util.BarcodeGenerator
import com.example.util.QRFormatUtils
import com.example.util.QRGenerator
import com.example.util.ScanResultData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GenTabType {
    QR_CODE,
    BARCODE
}

class QRMasterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = QRCodeRepository(db.qrCodeDao())

    private val prefs = application.getSharedPreferences("qr_master_prefs", Context.MODE_PRIVATE)

    // App Global Configs
    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))
    val onboardingCompleted = _onboardingCompleted.asStateFlow()

    private val _themeDark = MutableStateFlow(prefs.getBoolean("theme_dark", true)) // Default dark scheme for luxury layout
    val themeDark = _themeDark.asStateFlow()

    private val _beepAndVibrateEnabled = MutableStateFlow(prefs.getBoolean("beep_vibrate", true))
    val beepAndVibrateEnabled = _beepAndVibrateEnabled.asStateFlow()

    private val _beepEnabled = MutableStateFlow(prefs.getBoolean("beep_enabled", prefs.getBoolean("beep_vibrate", true)))
    val beepEnabled = _beepEnabled.asStateFlow()

    private val _vibrateEnabled = MutableStateFlow(prefs.getBoolean("vibrate_enabled", prefs.getBoolean("beep_vibrate", true)))
    val vibrateEnabled = _vibrateEnabled.asStateFlow()

    private val _autoSaveScanned = MutableStateFlow(prefs.getBoolean("auto_save_scanned", true))
    val autoSaveScanned = _autoSaveScanned.asStateFlow()

    private val _qrExportQuality = MutableStateFlow(prefs.getInt("qr_export_size", 512))
    val qrExportQuality = _qrExportQuality.asStateFlow()

    private val _passcode = MutableStateFlow(prefs.getString("app_passcode", null))
    val passcode = _passcode.asStateFlow()

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        _onboardingCompleted.value = true
    }

    fun toggleTheme(isDark: Boolean) {
        prefs.edit().putBoolean("theme_dark", isDark).apply()
        _themeDark.value = isDark
    }

    fun toggleBeepAndVibrate(enabled: Boolean) {
        prefs.edit().putBoolean("beep_vibrate", enabled).apply()
        _beepAndVibrateEnabled.value = enabled
        toggleBeep(enabled)
        toggleVibrate(enabled)
    }

    fun toggleBeep(enabled: Boolean) {
        prefs.edit().putBoolean("beep_enabled", enabled).apply()
        _beepEnabled.value = enabled
    }

    fun toggleVibrate(enabled: Boolean) {
        prefs.edit().putBoolean("vibrate_enabled", enabled).apply()
        _vibrateEnabled.value = enabled
    }

    fun toggleAutoSave(enabled: Boolean) {
        prefs.edit().putBoolean("auto_save_scanned", enabled).apply()
        _autoSaveScanned.value = enabled
    }

    fun setQrExportQuality(quality: Int) {
        prefs.edit().putInt("qr_export_size", quality).apply()
        _qrExportQuality.value = quality
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", false).apply()
        _onboardingCompleted.value = false
    }

    fun setPasscode(pin: String?) {
        prefs.edit().putString("app_passcode", pin).apply()
        _passcode.value = pin
    }

    // LIST / HISTORY STATE & QUERIES
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("ALL") // ALL, SCANNED, GENERATED, FAVORITES
    val filterType = _filterType.asStateFlow()

    val historyQRCodes: StateFlow<List<QRCodeEntity>> = combine(
        repository.allQRCodes,
        _searchQuery,
        _filterType
    ) { list, query, filter ->
        var resultList = list
        if (query.isNotEmpty()) {
            resultList = resultList.filter {
                it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
            }
        }
        when (filter) {
            "SCANNED" -> resultList = resultList.filter { it.isScanned }
            "GENERATED" -> resultList = resultList.filter { !it.isScanned }
            "FAVORITES" -> resultList = resultList.filter { it.isFavorite }
        }
        resultList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // SAVE/ACTIONS DB
    fun saveQRCode(qrCode: QRCodeEntity) {
        viewModelScope.launch {
            repository.insert(qrCode)
        }
    }

    fun updateQRCode(qrCode: QRCodeEntity) {
        viewModelScope.launch {
            repository.update(qrCode)
        }
    }

    fun deleteQRCode(qrCode: QRCodeEntity) {
        viewModelScope.launch {
            repository.delete(qrCode)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: String) {
        _filterType.value = filter
    }

    // REAL-TIME SHARING & SCANNER CONTROLS
    private val _scanResult = MutableStateFlow<ScanResultData?>(null)
    val scanResult = _scanResult.asStateFlow()

    fun setScanResult(result: ScanResultData?) {
        _scanResult.value = result
        if (result != null) {
            playScanFeedback()
            if (_autoSaveScanned.value) {
                // Auto save scanned code to history
                val isQr = result.format == "QR_CODE"
                val context = getApplication<Application>()
                val info = QRFormatUtils.parseTypeAndDescription(context, result.content)
                
                val mainType = if (isQr) info.first else "BARCODE"
                val titleText = if (isQr) info.second else context.getString(com.example.R.string.type_barcode, result.format)

                val entity = QRCodeEntity(
                    content = result.content,
                    type = mainType,
                    title = titleText,
                    isScanned = true,
                    qrStyle = result.format, // use qrStyle to hold the barcode format name
                    eyeStyle = if (isQr) "SQUARE" else "SHOW_TEXT"
                )
                saveQRCode(entity)
            }
        }
    }

    fun clearScanResult() {
        _scanResult.value = null
    }

    private fun playScanFeedback() {
        // Haptic Feedback
        if (_vibrateEnabled.value) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(120)
                }
            }
        }

        // Sound Feedback
        if (_beepEnabled.value) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // LIVE QR GENERATOR CONTROLLER
    private val _genContent = MutableStateFlow("https://qrmaster.app")
    val genContent = _genContent.asStateFlow()

    private val _genType = MutableStateFlow("URL") // URL, TEXT, WIFI, PHONE, SMS, EMAIL, VCARD, LOCATION, SOCIAL
    val genType = _genType.asStateFlow()

    // QR Style Specs
    private val _primaryColorHex = MutableStateFlow("#6366F1") // Luxury Indigo
    val primaryColorHex = _primaryColorHex.asStateFlow()

    private val _secondaryColorHex = MutableStateFlow("#06B6D4") // Luxury Cyan
    val secondaryColorHex = _secondaryColorHex.asStateFlow()

    private val _isGradient = MutableStateFlow(true)
    val isGradient = _isGradient.asStateFlow()

    private val _qrStyle = MutableStateFlow(QRGenerator.QrStyle.SQUARE)
    val qrStyle = _qrStyle.asStateFlow()

    private val _eyeStyle = MutableStateFlow(QRGenerator.EyeStyle.SQUARE)
    val eyeStyle = _eyeStyle.asStateFlow()

    private val _logoType = MutableStateFlow(QRGenerator.LogoType.NONE)
    val logoType = _logoType.asStateFlow()

    // Rendered output image state
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap = _previewBitmap.asStateFlow()

    // BARCODE MODE GENERATOR CONTROLLER
    private val _genTabType = MutableStateFlow(GenTabType.QR_CODE)
    val genTabType = _genTabType.asStateFlow()

    private val _barcodeContent = MutableStateFlow("123456789012")
    val barcodeContent = _barcodeContent.asStateFlow()

    private val _barcodeType = MutableStateFlow(BarcodeGenerator.BarcodeType.CODE_128)
    val barcodeType = _barcodeType.asStateFlow()

    private val _barcodePrimaryColorHex = MutableStateFlow("#0F172A") // Professional Slate/Black
    val barcodePrimaryColorHex = _barcodePrimaryColorHex.asStateFlow()

    private val _barcodeBgColorHex = MutableStateFlow("#FFFFFF") // Professional Clean White
    val barcodeBgColorHex = _barcodeBgColorHex.asStateFlow()

    private val _barcodeShowText = MutableStateFlow(true)
    val barcodeShowText = _barcodeShowText.asStateFlow()

    private val _barcodePreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val barcodePreviewBitmap = _barcodePreviewBitmap.asStateFlow()

    init {
        // Automatically re-estimate previews in background upon inputs alteration
        viewModelScope.launch {
            combine(
                _genContent,
                _primaryColorHex,
                _secondaryColorHex,
                _isGradient,
                _qrStyle,
                _eyeStyle,
                _logoType,
                _qrExportQuality
            ) { array ->
                val content = array[0] as String
                val prim = array[1] as String
                val sec = array[2] as String
                val grad = array[3] as Boolean
                val style = array[4] as QRGenerator.QrStyle
                val eye = array[5] as QRGenerator.EyeStyle
                val logo = array[6] as QRGenerator.LogoType
                val size = array[7] as Int
                QRGenParams(content, prim, sec, grad, style, eye, logo, size)
            }.collect { params ->
                regeneratePreview(params)
            }
        }

        // Automatically re-estimate barcode previews in background
        viewModelScope.launch {
            combine(
                _barcodeContent,
                _barcodeType,
                _barcodePrimaryColorHex,
                _barcodeBgColorHex,
                _barcodeShowText
            ) { array ->
                val content = array[0] as String
                val type = array[1] as BarcodeGenerator.BarcodeType
                val prim = array[2] as String
                val bg = array[3] as String
                val show = array[4] as Boolean
                BarcodeGenParams(content, type, prim, bg, show)
            }.collect { params ->
                regenerateBarcodePreview(params)
            }
        }
    }

    private data class QRGenParams(
        val content: String,
        val primaryColorHex: String,
        val secondaryColorHex: String,
        val isGradient: Boolean,
        val qrStyle: QRGenerator.QrStyle,
        val eyeStyle: QRGenerator.EyeStyle,
        val logoType: QRGenerator.LogoType,
        val size: Int
    )

    private data class BarcodeGenParams(
        val content: String,
        val type: BarcodeGenerator.BarcodeType,
        val primaryColorHex: String,
        val secondaryColorHex: String,
        val showText: Boolean
    )

    private suspend fun regeneratePreview(params: QRGenParams) {
        if (params.content.isEmpty()) {
            _previewBitmap.value = null
            return
        }
        withContext(Dispatchers.Default) {
            val b = QRGenerator.generate(
                content = params.content,
                size = params.size,
                primaryColorHex = params.primaryColorHex,
                secondaryColorHex = params.secondaryColorHex,
                isGradient = params.isGradient,
                qrStyle = params.qrStyle,
                eyeStyle = params.eyeStyle,
                logoType = params.logoType
            )
            _previewBitmap.value = b
        }
    }

    private suspend fun regenerateBarcodePreview(params: BarcodeGenParams) {
        if (params.content.isEmpty()) {
            _barcodePreviewBitmap.value = null
            return
        }
        withContext(Dispatchers.Default) {
            val b = BarcodeGenerator.generate(
                content = params.content,
                type = params.type,
                width = 800,
                height = 400,
                primaryHex = params.primaryColorHex,
                backgroundHex = params.secondaryColorHex,
                showText = params.showText
            )
            _barcodePreviewBitmap.value = b
        }
    }

    fun setGenTabType(type: GenTabType) {
        _genTabType.value = type
    }

    fun updateGenContent(content: String, type: String = _genType.value) {
        _genContent.value = content
        _genType.value = type
    }

    fun updateBarcodeContent(content: String) {
        _barcodeContent.value = content
    }

    fun updateBarcodeType(type: BarcodeGenerator.BarcodeType) {
        _barcodeType.value = type
    }

    fun updateBarcodeDesign(
        primaryHex: String = _barcodePrimaryColorHex.value,
        bgHex: String = _barcodeBgColorHex.value,
        showText: Boolean = _barcodeShowText.value
    ) {
        _barcodePrimaryColorHex.value = primaryHex
        _barcodeBgColorHex.value = bgHex
        _barcodeShowText.value = showText
    }

    fun updateDesign(
        primaryHex: String = _primaryColorHex.value,
        secondaryHex: String = _secondaryColorHex.value,
        gradient: Boolean = _isGradient.value,
        style: QRGenerator.QrStyle = _qrStyle.value,
        eye: QRGenerator.EyeStyle = _eyeStyle.value,
        logo: QRGenerator.LogoType = _logoType.value
    ) {
        _primaryColorHex.value = primaryHex
        _secondaryColorHex.value = secondaryHex
        _isGradient.value = gradient
        _qrStyle.value = style
        _eyeStyle.value = eye
        _logoType.value = logo
    }

    fun saveCurrentGeneratedCode(title: String) {
        viewModelScope.launch {
            val entity = QRCodeEntity(
                content = _genContent.value,
                type = _genType.value,
                title = title,
                isScanned = false,
                primaryColorHex = _primaryColorHex.value,
                secondaryColorHex = _secondaryColorHex.value,
                isGradient = _isGradient.value,
                qrStyle = _qrStyle.value.name,
                eyeStyle = _eyeStyle.value.name,
                logoType = _logoType.value.name
            )
            saveQRCode(entity)
        }
    }

    fun saveCurrentGeneratedBarcode(title: String) {
        viewModelScope.launch {
            val entity = QRCodeEntity(
                content = _barcodeContent.value,
                type = "BARCODE",
                title = title,
                isScanned = false,
                primaryColorHex = _barcodePrimaryColorHex.value,
                secondaryColorHex = _barcodeBgColorHex.value,
                isGradient = false,
                qrStyle = _barcodeType.value.name,
                eyeStyle = if (_barcodeShowText.value) "SHOW_TEXT" else "HIDE_TEXT",
                logoType = "NONE"
            )
            saveQRCode(entity)
        }
    }
}

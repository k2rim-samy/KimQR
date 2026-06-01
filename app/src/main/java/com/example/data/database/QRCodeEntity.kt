package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "qr_codes")
data class QRCodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val type: String, // TEXT, URL, WIFI, PHONE, SMS, EMAIL, VCARD, LOCATION, SOCIAL
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isScanned: Boolean, // true = scanned, false = generated
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    
    // Customization styles
    val primaryColorHex: String = "#000000",
    val secondaryColorHex: String = "#000000", // For gradient
    val isGradient: Boolean = false,
    val qrStyle: String = "SQUARE", // SQUARE, DOTS, ROUNDED
    val eyeStyle: String = "SQUARE", // SQUARE, CIRCLE, ROUNDED
    val logoType: String = "NONE", // NONE, WIFI, FB, INSTA, TELEGRAM, WA, GOOGLE, CUSTOM
    val passwordProtected: String? = null // Passcode if locked
) : Serializable

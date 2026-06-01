package com.example.util

import android.content.Context
import com.example.R

object QRFormatUtils {

    fun formatWiFi(ssid: String, password: String, security: String): String {
        val type = if (security == "None") "nopass" else security
        return "WIFI:S:$ssid;T:$type;P:$password;;"
    }

    fun formatPhone(number: String): String {
        return "tel:$number"
    }

    fun formatSMS(number: String, message: String): String {
        return "smsto:$number:$message"
    }

    fun formatEmail(email: String, subject: String, body: String): String {
        val s = subject.replace(" ", "%20")
        val b = body.replace(" ", "%20")
        return "mailto:$email?subject=$s&body=$b"
    }

    fun formatLocation(lat: String, lon: String): String {
        return "geo:$lat,$lon"
    }

    fun formatvCard(
        name: String,
        phone: String,
        email: String,
        org: String,
        title: String,
        url: String
    ): String {
        return """
            BEGIN:VCARD
            VERSION:3.0
            FN:$name
            ORG:$org
            TITLE:$title
            TEL:$phone
            EMAIL:$email
            URL:$url
            END:VCARD
        """.trimIndent()
    }

    /**
     * Determines the human readable content type and nice description
     */
    fun parseTypeAndDescription(context: Context, content: String): Pair<String, String> {
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("WIFI:", ignoreCase = true) -> {
                val ssid = trimmed.substringAfter("S:").substringBefore(";")
                "WIFI" to context.getString(R.string.type_wifi, ssid)
            }
            trimmed.startsWith("mailto:", ignoreCase = true) -> {
                val email = trimmed.substringAfter("mailto:").substringBefore("?")
                "EMAIL" to context.getString(R.string.type_email, email)
            }
            trimmed.startsWith("tel:", ignoreCase = true) -> {
                val num = trimmed.substringAfter("tel:")
                "PHONE" to context.getString(R.string.type_phone, num)
            }
            trimmed.startsWith("smsto:", ignoreCase = true) -> {
                val parts = trimmed.substringAfter("smsto:").split(":")
                val num = parts.getOrNull(0) ?: ""
                "SMS" to context.getString(R.string.type_sms, num)
            }
            trimmed.startsWith("geo:", ignoreCase = true) -> {
                val latLon = trimmed.substringAfter("geo:")
                "LOCATION" to context.getString(R.string.type_location, latLon)
            }
            trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) -> {
                val name = trimmed.lines().firstOrNull { it.startsWith("FN:") }?.substringAfter("FN:") ?: "Contact Card"
                "VCARD" to context.getString(R.string.type_vcard, name)
            }
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                val domain = trimmed.substringAfter("://").substringBefore("/")
                "URL" to context.getString(R.string.type_url, domain)
            }
            else -> {
                val textSnippet = "${trimmed.take(30)}${if (trimmed.length > 30) "..." else ""}"
                "TEXT" to context.getString(R.string.type_text, textSnippet)
            }
        }
    }
}

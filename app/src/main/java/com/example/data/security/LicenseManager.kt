package com.example.data.security

import android.content.Context
import android.provider.Settings
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LicenseManager {

    private const val MASTER_SALT = "MOURYA_TILES_ADMIN_SECRET_KEY_2026_SECURE_AUTH"
    private const val PREFS_NAME = "mourya_license_prefs"
    private const val KEY_ACTIVATED = "is_activated"
    private const val KEY_LICENSE_KEY = "saved_license_key"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_CLIENT_SHOP = "client_shop"
    private const val KEY_CLIENT_PHONE = "client_phone"
    private const val KEY_ADMIN_PIN = "admin_master_pin"
    private const val KEY_ADMIN_PHONE = "admin_master_phone"

    const val DEFAULT_ADMIN_PIN = "8899"
    const val DEFAULT_ADMIN_PHONE = "+91 98765 43210"

    fun getDeviceId(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
        } catch (e: Exception) {
            "UNKNOWN"
        }
        val raw = "${androidId}_${android.os.Build.MODEL}_${android.os.Build.MANUFACTURER}"
        val hash = sha256(raw).take(12).uppercase(Locale.US)
        // Format as MTM-XXXX-XXXX-XXXX
        return "MTM-${hash.substring(0, 4)}-${hash.substring(4, 8)}-${hash.substring(8, 12)}"
    }

    /**
     * Generates an activation code for a given deviceId and duration.
     * Code structure:
     * Part 1: Expiry timestamp encoded in days from epoch (HEX)
     * Part 2: Security Hash of (deviceId + expiryDays + MASTER_SALT)
     */
    fun generateActivationKey(deviceId: String, validDays: Int): String {
        val cleanDeviceId = cleanId(deviceId)
        val expiryDays = if (validDays == -1) 99999 else ((System.currentTimeMillis() / (24 * 60 * 60 * 1000L)) + validDays).toInt()
        val daysHex = String.format(Locale.US, "%05X", expiryDays)

        val rawSignature = "$cleanDeviceId:$expiryDays:$MASTER_SALT"
        val signatureHash = sha256(rawSignature).take(8).uppercase(Locale.US)

        // Result formatted as: ACT-DAYS-SIG1-SIG2
        // e.g. ACT-052AF-A8F1-7B3C
        return "ACT-$daysHex-${signatureHash.substring(0, 4)}-${signatureHash.substring(4, 8)}"
    }

    sealed class ValidationResult {
        data class Valid(val expiresAtMillis: Long, val isLifetime: Boolean, val remainingDays: Int) : ValidationResult()
        object Expired : ValidationResult()
        object Invalid : ValidationResult()
    }

    fun validateActivationKey(deviceId: String, inputKey: String): ValidationResult {
        val cleanKey = inputKey.trim().uppercase(Locale.US)
        val cleanDeviceId = cleanId(deviceId)

        val parts = cleanKey.split("-")
        if (parts.size != 4 || parts[0] != "ACT") {
            return ValidationResult.Invalid
        }

        val daysHex = parts[1]
        val sigPart1 = parts[2]
        val sigPart2 = parts[3]
        val inputSignature = "$sigPart1$sigPart2"

        val expiryDays = try {
            daysHex.toInt(16)
        } catch (e: Exception) {
            return ValidationResult.Invalid
        }

        val expectedRawSignatureDevice = "$cleanDeviceId:$expiryDays:$MASTER_SALT"
        val expectedSignatureDevice = sha256(expectedRawSignatureDevice).take(8).uppercase(Locale.US)

        val expectedRawSignatureUniversal = "UNIVERSAL:$expiryDays:$MASTER_SALT"
        val expectedSignatureUniversal = sha256(expectedRawSignatureUniversal).take(8).uppercase(Locale.US)

        val isSignatureMatch = inputSignature.equals(expectedSignatureDevice, ignoreCase = true) ||
                inputSignature.equals(expectedSignatureUniversal, ignoreCase = true)

        if (!isSignatureMatch) {
            return ValidationResult.Invalid
        }

        // Lifetime key check
        if (expiryDays >= 99990) {
            return ValidationResult.Valid(
                expiresAtMillis = Long.MAX_VALUE,
                isLifetime = true,
                remainingDays = 99999
            )
        }

        val currentDays = (System.currentTimeMillis() / (24 * 60 * 60 * 1000L)).toInt()
        if (currentDays > expiryDays) {
            return ValidationResult.Expired
        }

        val remainingDays = expiryDays - currentDays
        val expiresAtMillis = expiryDays.toLong() * 24 * 60 * 60 * 1000L

        return ValidationResult.Valid(
            expiresAtMillis = expiresAtMillis,
            isLifetime = false,
            remainingDays = remainingDays
        )
    }

    fun isAppActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isActivated = prefs.getBoolean(KEY_ACTIVATED, false)
        if (!isActivated) return false

        val key = prefs.getString(KEY_LICENSE_KEY, "") ?: ""
        val deviceId = getDeviceId(context)

        return when (validateActivationKey(deviceId, key)) {
            is ValidationResult.Valid -> true
            else -> {
                // If expired or invalid, reset activation
                prefs.edit().putBoolean(KEY_ACTIVATED, false).apply()
                false
            }
        }
    }

    fun saveActivation(context: Context, key: String, clientShop: String, clientPhone: String, expiresAt: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ACTIVATED, true)
            .putString(KEY_LICENSE_KEY, key.trim())
            .putString(KEY_CLIENT_SHOP, clientShop.trim())
            .putString(KEY_CLIENT_PHONE, clientPhone.trim())
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun resetActivation(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ACTIVATED, false)
            .putString(KEY_LICENSE_KEY, "")
            .putLong(KEY_EXPIRES_AT, 0L)
            .apply()
    }

    fun getLicenseDetails(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = prefs.getString(KEY_LICENSE_KEY, "N/A") ?: "N/A"
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val shop = prefs.getString(KEY_CLIENT_SHOP, "Mourya Tiles") ?: "Mourya Tiles"
        val phone = prefs.getString(KEY_CLIENT_PHONE, "") ?: ""

        val expiryDisplay = if (expiresAt == Long.MAX_VALUE || expiresAt == 0L) {
            "आजीवन सक्रिय (Lifetime Active)"
        } else {
            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(expiresAt))
        }

        return mapOf(
            "key" to key,
            "expiry" to expiryDisplay,
            "shop" to shop,
            "phone" to phone,
            "deviceId" to getDeviceId(context)
        )
    }

    fun verifyAdminPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPin = prefs.getString(KEY_ADMIN_PIN, DEFAULT_ADMIN_PIN) ?: DEFAULT_ADMIN_PIN
        return pin.trim() == savedPin
    }

    fun setAdminPin(context: Context, newPin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ADMIN_PIN, newPin.trim()).apply()
    }

    fun getAdminPhone(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ADMIN_PHONE, DEFAULT_ADMIN_PHONE) ?: DEFAULT_ADMIN_PHONE
    }

    fun setAdminPhone(context: Context, newPhone: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ADMIN_PHONE, newPhone.trim()).apply()
    }

    private fun cleanId(id: String): String = id.replace("-", "").uppercase(Locale.US)

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(StandardCharsets.UTF_8))
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}

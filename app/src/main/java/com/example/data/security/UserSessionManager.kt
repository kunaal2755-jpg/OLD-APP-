package com.example.data.security

import android.content.Context
import com.example.data.model.AppUser

object UserSessionManager {

    private const val PREFS_NAME = "mourya_user_session_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_SHOP_NAME = "shop_name"
    private const val KEY_USER_PHONE = "user_phone"
    private const val KEY_USER_PASSWORD = "user_password"
    private const val KEY_PLAN_NAME = "plan_name"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_APPROVAL_STATUS = "approval_status"
    private const val KEY_ADDRESS = "address"

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun saveSession(context: Context, user: AppUser) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_SHOP_NAME, user.shopName)
            .putString(KEY_USER_PHONE, user.phone)
            .putString(KEY_USER_PASSWORD, user.password)
            .putString(KEY_PLAN_NAME, user.planName)
            .putLong(KEY_EXPIRES_AT, user.expiresAt)
            .putString(KEY_APPROVAL_STATUS, user.approvalStatus)
            .putString(KEY_ADDRESS, user.address)
            .apply()
    }

    fun getCurrentUser(context: Context): AppUser? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) return null
        return AppUser(
            id = prefs.getLong(KEY_USER_ID, 0L),
            name = prefs.getString(KEY_USER_NAME, "दुकानदार") ?: "दुकानदार",
            shopName = prefs.getString(KEY_SHOP_NAME, "TILES AND MARBLES") ?: "TILES AND MARBLES",
            phone = prefs.getString(KEY_USER_PHONE, "") ?: "",
            password = prefs.getString(KEY_USER_PASSWORD, "") ?: "",
            planName = prefs.getString(KEY_PLAN_NAME, "1 Year") ?: "1 Year",
            expiresAt = prefs.getLong(KEY_EXPIRES_AT, Long.MAX_VALUE),
            approvalStatus = prefs.getString(KEY_APPROVAL_STATUS, "APPROVED") ?: "APPROVED",
            address = prefs.getString(KEY_ADDRESS, "") ?: ""
        )
    }

    fun updatePassword(context: Context, newPass: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_PASSWORD, newPass.trim()).apply()
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

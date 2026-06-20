package com.example.gymlog2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MonetizationRepository(private val db: AppDatabase) {
    suspend fun isSubscribed(userId: String): Boolean {
        val s = db.subscriptionDao().getForUser(userId)
        return s != null && s.status == "active" && s.currentPeriodEnd > System.currentTimeMillis()
    }
    suspend fun getSubscription(userId: String) = db.subscriptionDao().getForUser(userId)
    suspend fun seedActiveSubscription(userId: String, planId: String, periodEndMillis: Long) {
        db.subscriptionDao().upsert(SubscriptionEntity(userId = userId, subscriptionId = "local-demo", planId = planId, status = "active", currentPeriodEnd = periodEndMillis))
    }
}

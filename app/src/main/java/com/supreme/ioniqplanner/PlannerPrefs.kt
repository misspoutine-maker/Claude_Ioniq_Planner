package com.supreme.ioniqplanner

import android.content.Context

/** Planner settings + stats learned from imported Supreme Ioniq logs. */
data class LearnedStats(
    val avgWhKm: Double?, val totalKm: Double, val tripCount: Int,
    val avgChargeKw: Double?, val maxChargeKw: Double?, val chargeCount: Int
)

class PlannerPrefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("planner", Context.MODE_PRIVATE)

    var arrivalSoc: Float get() = p.getFloat("arrivalSoc", 10f); set(v) = p.edit().putFloat("arrivalSoc", v).apply()
    var departSoc: Float get() = p.getFloat("departSoc", 80f); set(v) = p.edit().putFloat("departSoc", v).apply()
    var minKw: Float get() = p.getFloat("minKw", 150f); set(v) = p.edit().putFloat("minKw", v).apply()
    var batteryKwh: Float get() = p.getFloat("batteryKwh", 77.4f); set(v) = p.edit().putFloat("batteryKwh", v).apply()

    fun saveStats(s: LearnedStats) {
        p.edit()
            .putFloat("avgWhKm", (s.avgWhKm ?: -1.0).toFloat())
            .putFloat("totalKm", s.totalKm.toFloat())
            .putInt("tripCount", s.tripCount)
            .putFloat("avgChargeKw", (s.avgChargeKw ?: -1.0).toFloat())
            .putFloat("maxChargeKw", (s.maxChargeKw ?: -1.0).toFloat())
            .putInt("chargeCount", s.chargeCount)
            .putBoolean("hasStats", true)
            .apply()
    }

    fun loadStats(): LearnedStats? {
        if (!p.getBoolean("hasStats", false)) return null
        fun d(k: String) = p.getFloat(k, -1f).let { if (it < 0f) null else it.toDouble() }
        return LearnedStats(d("avgWhKm"), p.getFloat("totalKm", 0f).toDouble(), p.getInt("tripCount", 0),
            d("avgChargeKw"), d("maxChargeKw"), p.getInt("chargeCount", 0))
    }
}

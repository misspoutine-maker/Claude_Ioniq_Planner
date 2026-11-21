package com.supreme.ioniqplanner

import org.json.JSONArray

/** Reads trips.json written by Supreme Ioniq 6 Logger and computes learned stats. */
object TripImporter {
    fun parse(text: String): LearnedStats {
        val arr = JSONArray(text)
        var energyWh = 0.0; var km = 0.0; var trips = 0
        val chargeKw = mutableListOf<Double>(); var maxKw = 0.0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            when (o.optString("type")) {
                "TRIP" -> {
                    val dist = o.optDouble("endKm", 0.0) - o.optDouble("startKm", 0.0)
                    val e = if (o.has("energyKwh")) o.optDouble("energyKwh", 0.0) else 0.0
                    if (dist > 5.0 && e > 0.0) { km += dist; energyWh += e * 1000.0; trips++ }
                }
                "CHARGE", "CHARGE_MEAL" -> {
                    if (o.has("avgChargeKw")) {
                        val a = o.optDouble("avgChargeKw", 0.0)
                        if (a > 5.0) chargeKw.add(a)
                    }
                    if (o.has("maxChargeKw")) maxKw = maxOf(maxKw, o.optDouble("maxChargeKw", 0.0))
                }
            }
        }
        return LearnedStats(
            avgWhKm = if (km > 0) energyWh / km else null,
            totalKm = km, tripCount = trips,
            avgChargeKw = if (chargeKw.isNotEmpty()) chargeKw.average() else null,
            maxChargeKw = if (maxKw > 0) maxKw else null,
            chargeCount = chargeKw.size
        )
    }
}

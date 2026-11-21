package com.supreme.ioniqplanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Place(val name: String, val lat: Double, val lon: Double)
data class RouteResult(val km: Double, val hours: Double, val points: List<Pair<Double, Double>>) // points = (lat, lon)

/** Free services: Nominatim (geocoding) + public OSRM demo server (routing). */
object Routing {
    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "SupremeIoniqPlanner/1.0")
        c.connectTimeout = 10000; c.readTimeout = 15000
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    suspend fun geocode(query: String): Place? = withContext(Dispatchers.IO) {
        try {
            val a = JSONArray(get("https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" +
                URLEncoder.encode(query, "UTF-8")))
            if (a.length() == 0) null else a.getJSONObject(0).let {
                Place(it.getString("display_name"), it.getString("lat").toDouble(), it.getString("lon").toDouble())
            }
        } catch (_: Exception) { null }
    }

    suspend fun route(a: Place, b: Place): RouteResult? = withContext(Dispatchers.IO) {
        try {
            val o = JSONObject(get("https://router.project-osrm.org/route/v1/driving/" +
                "${a.lon},${a.lat};${b.lon},${b.lat}?overview=simplified&geometries=geojson"))
            val r = o.getJSONArray("routes").getJSONObject(0)
            val coords = r.getJSONObject("geometry").getJSONArray("coordinates")
            val pts = (0 until coords.length()).map { i ->
                val c = coords.getJSONArray(i); Pair(c.getDouble(1), c.getDouble(0))
            }
            RouteResult(r.getDouble("distance") / 1000.0, r.getDouble("duration") / 3600.0, pts)
        } catch (_: Exception) { null }
    }
}

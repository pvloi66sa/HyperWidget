package com.hyperwidget

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class City(val name: String, val lat: Double, val lon: Double)

object Prefs {
    fun sp(ctx: Context) = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)
}

object Weather {
    val CITIES = listOf(
        City("Hà Nội", 21.0285, 105.8542), City("TP. Hồ Chí Minh", 10.8231, 106.6297),
        City("Đà Nẵng", 16.0544, 108.2022), City("Huế", 16.4637, 107.5909),
        City("Hải Phòng", 20.8449, 106.6881), City("Cần Thơ", 10.0452, 105.7469)
    )

    fun describe(code: Int): Pair<String, String> = when (code) {
        0 -> "☀️" to "Trời quang"
        1, 2 -> "🌤️" to "Ít mây"
        3 -> "☁️" to "Nhiều mây"
        45, 48 -> "🌫️" to "Sương mù"
        in 51..57 -> "🌦️" to "Mưa phùn"
        in 61..67 -> "🌧️" to "Mưa"
        in 71..77 -> "🌨️" to "Tuyết"
        in 80..82 -> "🌦️" to "Mưa rào"
        in 95..99 -> "⛈️" to "Dông"
        else -> "⛅" to "Có mây"
    }

    /** Gọi Open-Meteo và lưu cache. Trả về true nếu thành công. */
    fun refresh(ctx: Context): Boolean {
        return try {
            val sp = Prefs.sp(ctx)
            val c = CITIES[sp.getInt("city", 0).coerceIn(0, CITIES.size - 1)]
            val useGps = sp.getBoolean("gps", false) && sp.contains("glat")
            val lat = if (useGps) sp.getFloat("glat", 0f).toDouble() else c.lat
            val lon = if (useGps) sp.getFloat("glon", 0f).toDouble() else c.lon
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&timezone=auto&forecast_days=1"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val j = JSONObject(text)
            val cur = j.getJSONObject("current")
            val d = j.getJSONObject("daily")
            sp.edit()
                .putInt("t", Math.round(cur.getDouble("temperature_2m")).toInt())
                .putInt("code", cur.getInt("weather_code"))
                .putInt("hi", Math.round(d.getJSONArray("temperature_2m_max").getDouble(0)).toInt())
                .putInt("lo", Math.round(d.getJSONArray("temperature_2m_min").getDouble(0)).toInt())
                .putInt("rain", d.getJSONArray("precipitation_probability_max").optInt(0))
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }
}

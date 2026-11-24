package com.hyperwidget

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.widget.Switch
import android.widget.Toast
import java.util.Locale
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var spin: Spinner
    private lateinit var gpsSwitch: Switch
    private lateinit var gpsStatus: TextView

    private fun showStatus() {
        val sp = Prefs.sp(this)
        val on = sp.getBoolean("gps", false)
        gpsStatus.text = if (on) "Vị trí hiện tại: " + sp.getString("gname", "đang xác định...")
        else "Đang dùng thành phố đã chọn bên dưới."
        spin.isEnabled = !on
    }

    private fun locate() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 7)
            return
        }
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        for (pr in lm.getProviders(true)) {
            val l = try { lm.getLastKnownLocation(pr) } catch (e: SecurityException) { null }
            if (l != null && (best == null || l.accuracy < best.accuracy)) best = l
        }
        if (best != null) {
            useLocation(best)
            return
        }
        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> null
        }
        if (provider == null) {
            Toast.makeText(this, "Hãy bật dịch vụ vị trí của máy", Toast.LENGTH_LONG).show()
            gpsSwitch.isChecked = false
            return
        }
        gpsStatus.text = "Đang xác định vị trí..."
        try {
            lm.requestSingleUpdate(provider, object : LocationListener {
                override fun onLocationChanged(l: Location) { useLocation(l) }
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            }, Looper.getMainLooper())
        } catch (e: SecurityException) {
            gpsSwitch.isChecked = false
        }
    }

    private fun useLocation(l: Location) {
        val sp = Prefs.sp(this)
        val fallback = String.format(Locale.US, "%.2f, %.2f", l.latitude, l.longitude)
        sp.edit().putBoolean("gps", true).putFloat("glat", l.latitude.toFloat())
            .putFloat("glon", l.longitude.toFloat()).putString("gname", fallback).apply()
        showStatus()
        Scheduler.refreshNow(this)
        Thread {
            try {
                if (Geocoder.isPresent()) {
                    val list = Geocoder(this, Locale("vi")).getFromLocation(l.latitude, l.longitude, 1)
                    val a = list?.firstOrNull()
                    val name = listOfNotNull(a?.subAdminArea ?: a?.locality, a?.adminArea).distinct().joinToString(", ")
                    if (name.isNotEmpty()) {
                        sp.edit().putString("gname", name).apply()
                        runOnUiThread { showStatus() }
                    }
                }
            } catch (e: Exception) {
            }
        }.start()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == 7) {
            if (results.isNotEmpty() && results.any { it == PackageManager.PERMISSION_GRANTED }) locate()
            else {
                Toast.makeText(this, "Chưa được cấp quyền vị trí", Toast.LENGTH_LONG).show()
                gpsSwitch.isChecked = false
            }
        }
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(t: String, size: Float, bold: Boolean = false): TextView {
        val v = TextView(this)
        v.text = t
        v.textSize = size
        if (bold) v.setTypeface(null, Typeface.BOLD)
        v.setPadding(0, dp(14), 0, dp(4))
        return v
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sp = Prefs.sp(this)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(20), dp(48), dp(20), dp(32))

        col.addView(label("Widget Studio", 26f, true))
        col.addView(label("Nhấn giữ màn hình chính → Widget → chọn \"Widget Studio\" để thêm đồng hồ, thời tiết, lịch âm.", 14f))

        col.addView(label("Vị trí thời tiết", 16f, true))
        gpsSwitch = Switch(this)
        gpsSwitch.text = "Tự động theo GPS của máy"
        gpsStatus = label("", 13f)
        spin = Spinner(this)
        spin.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Weather.CITIES.map { it.name })
        spin.setSelection(sp.getInt("city", 0))
        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos != sp.getInt("city", 0)) {
                    sp.edit().putInt("city", pos).apply()
                    Scheduler.refreshNow(this@MainActivity)
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        gpsSwitch.isChecked = sp.getBoolean("gps", false)
        gpsSwitch.setOnCheckedChangeListener { _, on ->
            if (on) locate()
            else {
                sp.edit().putBoolean("gps", false).apply()
                showStatus()
                Scheduler.refreshNow(this)
            }
        }
        col.addView(gpsSwitch)
        col.addView(gpsStatus)
        col.addView(label("Hoặc chọn thành phố", 14f))
        col.addView(spin)
        showStatus()

        col.addView(label("Giao diện", 16f, true))
        val group = RadioGroup(this)
        val btns = listOf("Sáng", "Tối", "Kính mờ").map { n ->
            val b = RadioButton(this)
            b.text = n
            b.id = View.generateViewId()
            group.addView(b)
            b
        }
        btns[sp.getInt("theme", 2).coerceIn(0, 2)].isChecked = true
        group.setOnCheckedChangeListener { _, checked ->
            val idx = btns.indexOfFirst { it.id == checked }
            if (idx >= 0) {
                sp.edit().putInt("theme", idx).apply()
                Renderer.updateAll(this)
            }
        }
        col.addView(group)

        col.addView(label("Màu nhấn (ngày âm)", 16f, true))
        val accRow = LinearLayout(this)
        Renderer.ACC.forEachIndexed { i, c ->
            val b = Button(this)
            b.setBackgroundColor(c)
            b.layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).also { it.marginEnd = dp(10) }
            b.setOnClickListener {
                sp.edit().putInt("acc", i).apply()
                Renderer.updateAll(this)
            }
            accRow.addView(b)
        }
        col.addView(accRow)

        val refresh = Button(this)
        refresh.text = "Cập nhật thời tiết ngay"
        refresh.setOnClickListener {
            if (sp.getBoolean("gps", false)) locate() else Scheduler.refreshNow(this)
            Renderer.updateAll(this)
        }
        col.addView(refresh)

        col.addView(label("Để widget không bị đứng trên HyperOS", 16f, true))
        col.addView(label(
            "1. Cài đặt → Ứng dụng → Quyền → Tự khởi động: bật cho Widget Studio.\n" +
                "2. Cài đặt → Pin → Tiết kiệm pin ứng dụng: chọn Không hạn chế.\n" +
                "3. Mở đa nhiệm, nhấn giữ thẻ app và khóa lại.", 14f))
        val settings = Button(this)
        settings.text = "Mở cài đặt ứng dụng"
        settings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        col.addView(settings)

        val sv = ScrollView(this)
        sv.addView(col)
        setContentView(sv)
        Scheduler.ensure(this)
    }
}

package com.hyperwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.RemoteViews
import java.util.Calendar

object Renderer {
    val ACC = intArrayOf(
        0xFFE8553D.toInt(), 0xFF2F80ED.toInt(), 0xFF1FA971.toInt(), 0xFFA35BE0.toInt(), 0xFFE0A100.toInt()
    )
    private val WD = arrayOf("Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy")
    private val PROVIDERS = listOf(
        W1::class.java to R.layout.w1, W2::class.java to R.layout.w2, W3::class.java to R.layout.w3,
        W4::class.java to R.layout.w4, W5::class.java to R.layout.w5
    )

    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        for ((cls, layout) in PROVIDERS) {
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, cls))
            for (id in ids) mgr.updateAppWidget(id, build(ctx, layout))
        }
        CalRenderer.updateAll(ctx)
    }

    fun build(ctx: Context, layout: Int): RemoteViews {
        val rv = RemoteViews(ctx.packageName, layout)
        val sp = Prefs.sp(ctx)
        val theme = sp.getInt("theme", 2)
        val acc = ACC[sp.getInt("acc", 0).coerceIn(0, ACC.size - 1)]
        val (ink, mute, bg) = when (theme) {
            0 -> Triple(0xFF1D1C1B.toInt(), 0xFF6A665F.toInt(), R.drawable.bg_light)
            1 -> Triple(0xFFF4F1EC.toInt(), 0xFFAAA39A.toInt(), R.drawable.bg_dark)
            else -> Triple(0xFFFFFFFF.toInt(), 0xCCFFFFFF.toInt(), R.drawable.bg_glass)
        }
        val now = Calendar.getInstance()
        val d = now.get(Calendar.DAY_OF_MONTH)
        val m = now.get(Calendar.MONTH) + 1
        val y = now.get(Calendar.YEAR)
        val l = Lunar.convert(d, m, y)

        var canchi = "Ngày ${Lunar.dayName(l.jd)} · năm ${Lunar.yearName(l.year)}"
        for (i in 0..3) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_MONTH, i)
            val x = Lunar.convert(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
            if (x.day == 1 || x.day == 15) {
                val n = if (x.day == 1) "Mùng 1" else "Rằm"
                canchi += if (i == 0) " · Hôm nay $n" else " · $n còn $i ngày"
                break
            }
        }

        val hasW = sp.contains("t")
        val (icon, cond) = if (hasW) Weather.describe(sp.getInt("code", 0)) else ("⛅" to "Đang cập nhật")
        val temp = if (hasW) "${sp.getInt("t", 0)}°" else "--°"
        val extra = if (hasW) "$cond · ↑${sp.getInt("hi", 0)}° ↓${sp.getInt("lo", 0)}° · mưa ${sp.getInt("rain", 0)}%" else cond

        rv.setTextViewText(R.id.date, "${WD[now.get(Calendar.DAY_OF_WEEK) - 1]}, $d/$m")
        rv.setTextViewText(R.id.lunar, "${l.day}/${l.month}${if (l.leap) " nhuận" else ""} ÂL")
        rv.setTextViewText(R.id.lmonth, "Tháng ${l.month}${if (l.leap) " nhuận" else ""} ÂL")
        rv.setTextViewText(R.id.lday, l.day.toString())
        rv.setTextViewText(R.id.canchi, canchi)
        rv.setTextViewText(R.id.extra, extra)
        rv.setTextViewText(R.id.temp, temp)
        rv.setTextViewText(R.id.icon, icon)
        rv.setTextViewText(R.id.cond, cond)

        rv.setInt(R.id.root, "setBackgroundResource", bg)
        for (id in intArrayOf(R.id.clock, R.id.date, R.id.temp, R.id.icon)) rv.setTextColor(id, ink)
        for (id in intArrayOf(R.id.canchi, R.id.extra, R.id.cond)) rv.setTextColor(id, mute)
        for (id in intArrayOf(R.id.lunar, R.id.lmonth, R.id.lday)) rv.setTextColor(id, acc)

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), flags)
        val alarm = PendingIntent.getActivity(
            ctx, 1, Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags
        )
        rv.setOnClickPendingIntent(R.id.root, open)
        rv.setOnClickPendingIntent(R.id.clock, alarm)
        return rv
    }
}

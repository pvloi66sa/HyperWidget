package com.hyperwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import java.util.Calendar

object CalActions {
    const val PREV = "com.hyperwidget.CAL_PREV"
    const val NEXT = "com.hyperwidget.CAL_NEXT"
    const val TODAY = "com.hyperwidget.CAL_TODAY"
}

object CalRenderer {
    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        for (id in mgr.getAppWidgetIds(ComponentName(ctx, W6::class.java))) mgr.updateAppWidget(id, build(ctx))
    }

    private fun act(ctx: Context, code: Int, action: String): PendingIntent {
        val i = Intent(ctx, W6::class.java).setAction(action)
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun build(ctx: Context): RemoteViews {
        val rv = RemoteViews(ctx.packageName, R.layout.w6)
        val sp = Prefs.sp(ctx)
        val acc = Renderer.ACC[sp.getInt("acc", 0).coerceIn(0, Renderer.ACC.size - 1)]
        val (ink, mute, bg) = when (sp.getInt("theme", 2)) {
            0 -> Triple(0xFF1D1C1B.toInt(), 0xFF9A958E.toInt(), R.drawable.bg_light)
            1 -> Triple(0xFFF4F1EC.toInt(), 0xFF7D776F.toInt(), R.drawable.bg_dark)
            else -> Triple(0xFFFFFFFF.toInt(), 0x99FFFFFF.toInt(), R.drawable.bg_glass)
        }
        val showLunar = sp.getBoolean("cal_lunar", true)
        val off = sp.getInt("cal_off", 0)
        val today = Calendar.getInstance()
        val first = Calendar.getInstance()
        first.set(Calendar.DAY_OF_MONTH, 1)
        first.add(Calendar.MONTH, off)
        val year = first.get(Calendar.YEAR)
        val month = first.get(Calendar.MONTH)
        val dow = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val c = first.clone() as Calendar
        c.add(Calendar.DAY_OF_MONTH, -dow)

        for (i in 0..41) {
            val inMonth = c.get(Calendar.MONTH) == month
            val isToday = c.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            val lu = Lunar.convert(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
            rv.setTextViewText(CalIds.D[i], c.get(Calendar.DAY_OF_MONTH).toString())
            rv.setTextColor(CalIds.D[i], if (isToday) 0xFFFFFFFF.toInt() else if (inMonth) ink else mute)
            rv.setInt(CalIds.D[i], "setBackgroundResource", if (isToday) R.drawable.today_bg else 0)
            rv.setTextViewTextSize(CalIds.D[i], TypedValue.COMPLEX_UNIT_SP, if (showLunar) 9f else 11f)
            rv.setViewVisibility(CalIds.L[i], if (showLunar) View.VISIBLE else View.GONE)
            rv.setTextViewText(CalIds.L[i], if (lu.day == 1) "${lu.day}/${lu.month}" else lu.day.toString())
            rv.setTextColor(CalIds.L[i], if (inMonth && (lu.day == 1 || lu.day == 15)) acc else mute)
            c.add(Calendar.DAY_OF_MONTH, 1)
        }
        for (id in CalIds.WH) rv.setTextColor(id, mute)

        rv.setTextViewText(R.id.ctitle, "%d/%02d".format(year, month + 1))
        val ref = if (off == 0) today else (first.clone() as Calendar).also { it.set(Calendar.DAY_OF_MONTH, 15) }
        val l = Lunar.convert(ref.get(Calendar.DAY_OF_MONTH), ref.get(Calendar.MONTH) + 1, ref.get(Calendar.YEAR))
        rv.setTextViewText(R.id.clunar, if (off == 0) "${l.day}/${l.month}${if (l.leap) " nhuận" else ""} ÂL" else "Tháng ${l.month} ÂL")
        for (id in intArrayOf(R.id.ctitle, R.id.cprev, R.id.cnext)) rv.setTextColor(id, ink)
        rv.setTextColor(R.id.clunar, acc)
        rv.setInt(R.id.root, "setBackgroundResource", bg)

        rv.setOnClickPendingIntent(R.id.cprev, act(ctx, 11, CalActions.PREV))
        rv.setOnClickPendingIntent(R.id.cnext, act(ctx, 12, CalActions.NEXT))
        rv.setOnClickPendingIntent(R.id.ctitle, act(ctx, 13, CalActions.TODAY))
        val calIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        rv.setOnClickPendingIntent(
            R.id.root,
            PendingIntent.getActivity(ctx, 14, calIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        )
        return rv
    }
}

class W6 : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) mgr.updateAppWidget(id, CalRenderer.build(ctx))
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        val sp = Prefs.sp(ctx)
        val off = sp.getInt("cal_off", 0)
        when (intent.action) {
            CalActions.PREV -> sp.edit().putInt("cal_off", off - 1).apply()
            CalActions.NEXT -> sp.edit().putInt("cal_off", off + 1).apply()
            CalActions.TODAY -> sp.edit().putInt("cal_off", 0).apply()
            else -> return
        }
        CalRenderer.updateAll(ctx)
    }
}

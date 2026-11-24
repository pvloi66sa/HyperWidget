package com.hyperwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WeatherWorker(ctx: Context, p: WorkerParameters) : Worker(ctx, p) {
    override fun doWork(): Result {
        Weather.refresh(applicationContext)
        Renderer.updateAll(applicationContext)
        return Result.success()
    }
}

object Scheduler {
    private fun net() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun ensure(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<WeatherWorker>(30, TimeUnit.MINUTES).setConstraints(net()).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("wx", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun refreshNow(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<WeatherWorker>().setConstraints(net()).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("wx_now", ExistingWorkPolicy.REPLACE, req)
    }
}

abstract class BaseProvider(private val layout: Int) : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) mgr.updateAppWidget(id, Renderer.build(ctx, layout))
        Scheduler.ensure(ctx)
        Scheduler.refreshNow(ctx)
    }

    override fun onEnabled(ctx: Context) {
        Scheduler.ensure(ctx)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED ->
                Renderer.updateAll(ctx)
        }
    }
}

class W1 : BaseProvider(R.layout.w1)
class W2 : BaseProvider(R.layout.w2)
class W3 : BaseProvider(R.layout.w3)
class W4 : BaseProvider(R.layout.w4)
class W5 : BaseProvider(R.layout.w5)

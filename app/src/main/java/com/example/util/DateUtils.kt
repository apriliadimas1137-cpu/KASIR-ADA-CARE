package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val localeId = Locale("id", "ID")

    fun todayKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", localeId)
        return sdf.format(Date())
    }

    fun formatShortDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("d MMM yyyy", localeId)
        return sdf.format(Date(timestamp))
    }

    fun formatFullDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", localeId)
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("d MMM yyyy HH:mm", localeId)
        return sdf.format(Date(timestamp))
    }

    fun formatCurrency(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(localeId)
        format.maximumFractionDigits = 0
        return format.format(amount).replace(",00", "")
    }

    /**
     * Menghitung estimasi tanggal selesai berdasarkan:
     * - Durasi pengerjaan layanan terlama (base days)
     * - Beban antrian aktif vs kapasitas toko per hari
     * - Overload manual
     * - Hari libur toko
     */
    fun calculateEstimate(
        baseDurationDays: Int,
        activeQueuePairs: Int,
        newOrderPairs: Int,
        dailyCapacity: Int,
        autoQueueDelay: Boolean,
        overloadDays: Int,
        offDay: Int
    ): Pair<Long, String> {
        val cap = if (dailyCapacity > 0) dailyCapacity else 10
        val queueDelay = if (autoQueueDelay) {
            val totalPairs = activeQueuePairs + newOrderPairs
            val extra = (totalPairs / cap)
            maxOf(0, extra)
        } else 0

        val totalDaysToAdd = maxOf(1, baseDurationDays + queueDelay + overloadDays)

        val cal = Calendar.getInstance()
        var added = 0
        while (added < totalDaysToAdd) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0: Sun, 1: Mon, etc.
            if (offDay < 0 || dayOfWeek != offDay) {
                added++
            }
        }

        val estimatedTimestamp = cal.timeInMillis
        val reasonBuilder = StringBuilder("Pengerjaan $baseDurationDays hari kerja")
        if (queueDelay > 0) {
            reasonBuilder.append(" + $queueDelay hari (antrian padat $activeQueuePairs pasang)")
        }
        if (overloadDays > 0) {
            reasonBuilder.append(" + $overloadDays hari penyesuaian")
        }

        return Pair(estimatedTimestamp, reasonBuilder.toString())
    }
}

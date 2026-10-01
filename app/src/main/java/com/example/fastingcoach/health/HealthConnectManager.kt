package com.example.fastingcoach.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.fastingcoach.data.DailySteps
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectManager(private val context: Context) {
    val requiredPermissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    suspend fun hasPermissions(): Boolean {
        if (sdkStatus() != HealthConnectClient.SDK_AVAILABLE) return false
        return client().permissionController.getGrantedPermissions().containsAll(requiredPermissions)
    }

    suspend fun readSteps(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long {
        if (!hasPermissions()) return 0
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val result = client().aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return result[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    suspend fun readLast7Days(endDate: LocalDate = LocalDate.now()): List<DailySteps> {
        if (!hasPermissions()) return emptyList()
        return (0L..6L).map { offset ->
            val date = endDate.minusDays(offset)
            DailySteps(date.toEpochDay(), readSteps(date))
        }.sortedBy { it.epochDay }
    }
}

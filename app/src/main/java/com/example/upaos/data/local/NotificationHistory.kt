package com.example.upaos.data.local

import com.example.upaos.data.model.NotificacionItem
import com.example.upaos.data.model.NotificacionesResponse

/** Stable within a snapshot attempt, distinct across accounts and later legitimate changes. */
fun notificationEventKey(user: String, source: String, period: String?, generation: Long, identity: String): String =
    java.security.MessageDigest.getInstance("SHA-256")
        .digest("$user/$source/$period/$generation/$identity".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

fun notificationHistoryId(eventKey: String): Long = java.nio.ByteBuffer.wrap(
    java.security.MessageDigest.getInstance("SHA-256").digest(eventKey.toByteArray(Charsets.UTF_8))
).long

fun insertNotification(history: NotificacionesResponse, item: NotificacionItem): NotificacionesResponse {
    if (item.eventKey != null && history.notificaciones.any { it.eventKey == item.eventKey }) return history
    val items = listOf(item) + history.notificaciones
    return NotificacionesResponse(items.count { !it.leida }, items.size, items)
}

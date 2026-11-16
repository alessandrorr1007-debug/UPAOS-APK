package com.example.upaos.data.local

/** Bounds newly persisted navigation, discussion listings and file metadata; never document bytes. */
object CanvasResourceRetention {
    const val MAX_ENTRIES = 32
    const val MAX_BYTES = 256 * 1024
    const val MAX_AGE = 7 * 24 * 60 * 60_000L
    fun managed(flow: String) = listOf("navigation:", "topics:", "files:").any(flow::startsWith)
    fun fresh(saved: Long?, now: Long) = saved != null && now - saved in 0..MAX_AGE
    fun evict(times: Map<String, Long>, now: Long): Set<String> {
        val fresh = times.filterValues { fresh(it, now) }
        return (times.keys - fresh.keys) + fresh.entries.sortedByDescending { it.value }.drop(MAX_ENTRIES).map { it.key }
    }
}

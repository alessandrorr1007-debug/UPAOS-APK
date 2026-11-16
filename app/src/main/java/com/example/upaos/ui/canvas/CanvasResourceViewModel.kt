package com.example.upaos.ui.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.upaos.data.canvas.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CanvasResourceState<T>(
    val data: T,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val failure: CanvasError? = null,
    val updatedAt: Long? = null,
    val fromCache: Boolean = false
)

class CanvasResourceViewModel<T>(
    private val repository: CanvasResourceRepository<T>,
    initial: T,
    private val restore: (suspend () -> T?)? = null,
    private val restoreUpdatedAt: (suspend () -> Long?)? = null
) : ViewModel() {
    private val mutable = MutableStateFlow(CanvasResourceState(initial))
    val state = mutable.asStateFlow()
    private var request: Job? = null
    private var restored = false
    private var hasData = when (initial) { null -> false; is Collection<*> -> initial.isNotEmpty(); else -> true }
    init { refresh() }

    fun refresh(manual: Boolean = false) {
        if (request?.isActive == true) return
        request = viewModelScope.launch(start = CoroutineStart.LAZY) {
            mutable.value = state.value.copy(loading = !hasData && !manual, refreshing = hasData || manual, error = null, failure = null)
            try {
                if (!restored) {
                    restored = true
                    try { restore?.invoke()?.let { hasData = true; mutable.value = state.value.copy(data = it, loading = false, refreshing = true, fromCache = true, updatedAt = restoreUpdatedAt?.invoke()) } }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { /* A corrupt cache must not block the live request. */ }
                }
                val data = canvasReadWithBackoff { repository.load() }
                ensureActive()
                hasData = true
                mutable.value = state.value.copy(data = data, updatedAt = System.currentTimeMillis(), fromCache = false)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutable.value = state.value.copy(error = canvasError(e).text, failure = canvasError(e))
            } finally {
                mutable.value = state.value.copy(loading = false, refreshing = false)
            }
        }
        request!!.start()
    }
}

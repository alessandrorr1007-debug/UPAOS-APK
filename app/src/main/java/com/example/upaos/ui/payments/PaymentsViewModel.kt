package com.example.upaos.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.upaos.data.payments.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class PaymentsState(
    val overview: AccountOverview? = null,
    val period: PaymentPeriod? = null,
    val movements: PeriodMovements? = null,
    val loading: Boolean = true,
    val error: PaymentError? = null,
    val general: PeriodMovements? = null
)

internal class PaymentsViewModel(
    private val repository: PaymentsRepository,
    private val validateSession: () -> Unit = {}
) : ViewModel() {
    private val mutableState = MutableStateFlow(PaymentsState())
    val state = mutableState.asStateFlow()
    private var request: Job? = null

    fun refresh() {
        if (request?.isActive == true) return
        val previousCode = state.value.period?.code
        mutableState.value = PaymentsState()
        request = viewModelScope.launch {
            try {
                val overview = repository.overview()
                validateSession()
                val period = overview.periods.firstOrNull { it.code == previousCode } ?: overview.periods.firstOrNull()
                val generalPeriod = overview.periods.firstOrNull { it.code == ALL_TERMS }
                val general = generalPeriod?.let { repository.movements(overview, it) }
                currentCoroutineContext().ensureActive()
                validateSession()
                loadPeriod(overview, period, general)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                showError(error)
            }
        }
    }

    fun selectPeriod(period: PaymentPeriod) {
        val current = state.value
        val overview = current.overview ?: return
        if (period !in overview.periods || period == current.period) return
        request?.cancel()
        mutableState.value = PaymentsState(overview = overview, period = period, general = current.general)
        request = viewModelScope.launch { loadPeriod(overview, period, current.general) }
    }

    private suspend fun loadPeriod(overview: AccountOverview, period: PaymentPeriod?, general: PeriodMovements?) {
        mutableState.value = PaymentsState(overview = overview, period = period, general = general)
        try {
            val movements = if (period?.code == ALL_TERMS) general
                else period?.takeIf { it.canOpen }?.let { repository.movements(overview, it) }
            currentCoroutineContext().ensureActive()
            validateSession()
            mutableState.value = PaymentsState(overview, period, movements, loading = false, general = general)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            showError(error)
        }
    }

    private fun showError(exception: Exception) {
        val error = paymentError(exception)
        val safeState = when (error) {
            PaymentError.EXPIRED, PaymentError.UNAUTHORIZED -> PaymentsState()
            else -> state.value
        }
        mutableState.value = safeState.copy(loading = false, error = error)
    }

    fun clear() {
        request?.cancel()
        request = null
        mutableState.value = PaymentsState()
    }

    override fun onCleared() {
        clear()
        super.onCleared()
    }

    companion object {
        fun factory(repository: PaymentsRepository, validateSession: () -> Unit): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = PaymentsViewModel(repository, validateSession) as T
            }
    }
}

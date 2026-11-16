package com.example.upaos.data.payments

import com.example.upaos.data.upao.UpaoInvalidCredentialsException
import com.example.upaos.data.upao.UpaoNetworkException
import com.example.upaos.data.upao.UpaoServiceUnavailableException
import com.example.upaos.data.upao.UpaoSessionExpiredException
import com.example.upaos.data.upao.UpaoUnexpectedFormatException
import com.google.gson.JsonParseException
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

internal data class AccountOverview(
    val balance: String?,
    val due: String?,
    val date: String?,
    val currency: String?,
    val holds: Int?,
    val periods: List<PaymentPeriod>,
    val currencyCode: String,
    val noAmountDue: Boolean,
    val hasData: Boolean?,
    val hasTransactions: Boolean?
)

internal data class PaymentPeriod(val code: String, val label: String, val balance: String?, val canOpen: Boolean)
internal data class MovementAmounts(val charge: String?, val payment: String?, val balance: String?)
internal data class PaymentMovement(val code: String?, val label: String?, val date: String?, val amounts: MovementAmounts)
internal data class PeriodMovements(val items: List<PaymentMovement>, val totals: MovementAmounts?)

internal enum class PaymentError(val message: String) {
    CONNECTION("Error de conexión. Revisa tu acceso a internet."),
    EXPIRED("La sesión de Estado de cuenta expiró. Vuelve a iniciar sesión."),
    UNAUTHORIZED("La cuenta no tiene acceso a este servicio."),
    SERVER("El servidor no pudo completar la consulta."),
    UNAVAILABLE("El servicio está temporalmente no disponible."),
    RESPONSE("La respuesta de Estado de cuenta tiene un formato inesperado.")
}

internal fun paymentError(error: Exception): PaymentError = when (error) {
    is UpaoSessionExpiredException, is UpaoInvalidCredentialsException -> PaymentError.EXPIRED
    is UpaoServiceUnavailableException -> PaymentError.UNAVAILABLE
    is UpaoNetworkException -> PaymentError.CONNECTION
    is UpaoUnexpectedFormatException -> PaymentError.RESPONSE
    is PaymentHttpException -> httpPaymentError(error.status)
    is HttpException -> httpPaymentError(error.code())
    is IOException -> PaymentError.CONNECTION
    else -> PaymentError.RESPONSE
}

private fun httpPaymentError(status: Int): PaymentError = when (status) {
    401 -> PaymentError.EXPIRED
    403 -> PaymentError.UNAUTHORIZED
    429, 502, 503, 504 -> PaymentError.UNAVAILABLE
    in 500..599 -> PaymentError.SERVER
    else -> PaymentError.RESPONSE
}

private val unicodeSpaces = Regex("[\\p{Zs}]")

internal class PaymentsRepository(
    private val service: PaymentService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val renewSession: suspend () -> Boolean = { false }
) {
    suspend fun overview(): AccountOverview = withContext(dispatcher) {
        withSessionRenewal { loadOverview() }
    }

    private suspend fun loadOverview(): AccountOverview {
        val currencies = service.listForUIForStudent()
        requireResponse(currencies.success)
        val available = currencies.result.orEmpty()
        val currency = available.firstOrNull { it.isBase == true } ?: available.singleOrNull()
        val code = currency?.key.orEmpty()
        val balance = service.getBalanceAndConfigInfo(currencyCode = code, isOverview = true)
        val termBalances = service.getMyTermsAndTotals("")
        val terms = service.getMyTerms("")
        val holds = service.getHoldCount()
        requireResponse(termBalances.success)
        requireResponse(terms.success)
        val byCode = termBalances.termsTotals.orEmpty().filter { !it.termCode.isNullOrBlank() }
            .associateBy { it.termCode }
        val periods = terms.dropDown.orEmpty().mapNotNull { term ->
            val termCode = term.key?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val totals = byCode[termCode]
            PaymentPeriod(
                code = termCode,
                label = if (termCode == ALL_TERMS) "Todos los periodos"
                    else term.value.nonBlank() ?: totals?.termDescription.nonBlank() ?: termCode,
                balance = amount(totals?.totalBalanceDisplay, totals?.termTotalBalance),
                canOpen = totals?.cannotExpand != true
            )
        }.distinctBy { it.code }

        return AccountOverview(
            balance = amount(balance.totalBalanceDisplay, balance.totalBalance)
                .takeIf { balance.hasData != false && balance.showBalance != false },
            due = amount(balance.currentAmountDueDisplay, balance.currentAmountDue)
                .takeIf { balance.hasData != false && balance.showCurrentAmountDue != false },
            date = balance.asOfDate.nonBlank().takeIf { balance.hasAsOfDate != false },
            currency = currency?.iso.nonBlank() ?: currency?.value.nonBlank() ?: balance.currency.nonBlank() ?: code.nonBlank(),
            holds = holds.count?.takeIf { it >= 0 },
            periods = periods,
            currencyCode = code,
            noAmountDue = balance.hasData != false && balance.showCurrentAmountDue != false &&
                balance.currentAmountDue?.signum() == 0,
            hasData = balance.hasData,
            hasTransactions = balance.hasTransactions ?: currencies.hasTransactions
        )
    }

    suspend fun movements(overview: AccountOverview, period: PaymentPeriod): PeriodMovements = withContext(dispatcher) {
        require(period.canOpen && period in overview.periods && period.code.isNotBlank())
        val response = withSessionRenewal {
            if (period.code == ALL_TERMS) service.fetchDefaultAccountSummary(overview.currencyCode)
            else service.fetchAccountSummaryByTerm("", period.code)
        }
        requireResponse(response.success)
        PeriodMovements(
            items = response.result.orEmpty().map {
                PaymentMovement(
                    it.detailCode.nonBlank(), it.description.nonBlank(), it.date.nonBlank(),
                    MovementAmounts(
                        amount(it.chargeDisplay, it.charge),
                        amount(it.paymentDisplay, it.payment),
                        amount(it.balanceDisplay, it.balance)
                    )
                )
            },
            totals = response.totals?.let {
                MovementAmounts(it.totalchargeDisplay.nonBlank(), it.totalpaymentDisplay.nonBlank(), it.totalbalanceDisplay.nonBlank())
            }
        )
    }

    private suspend fun <T> withSessionRenewal(load: suspend () -> T): T = try {
        load()
    } catch (error: IOException) {
        if (paymentError(error) != PaymentError.EXPIRED || !renewSession()) throw error
        load()
    }

    private fun requireResponse(success: Boolean?) {
        if (success == false) throw JsonParseException("La consulta de pagos fue rechazada")
    }

    private fun amount(display: String?, value: BigDecimal?) = display.nonBlank() ?: value?.toPlainString()
    private fun String?.nonBlank() = this?.replace(unicodeSpaces, " ")?.trim()?.takeIf(String::isNotEmpty)
}

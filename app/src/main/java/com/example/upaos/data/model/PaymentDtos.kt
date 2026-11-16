package com.example.upaos.data.model

import com.google.gson.JsonElement
import java.math.BigDecimal

internal data class BalanceDto(
    val hasData: Boolean? = null,
    val totalBalanceDisplay: String? = null,
    val totalBalance: BigDecimal? = null,
    val currentAmountDue: BigDecimal? = null,
    val currentAmountDueDisplay: String? = null,
    val asOfDate: String? = null,
    val hasAsOfDate: Boolean? = null,
    val hasTransactions: Boolean? = null,
    val showBalance: Boolean? = null,
    val showCurrentAmountDue: Boolean? = null,
    val currency: String? = null,
    val data: BalanceConfigDto? = null,
    val showNoRecordsInfoText: Boolean? = null,
    val showDetailCode: Boolean? = null,
    val displayTermDateRange: Boolean? = null,
    val showStudyPath: Boolean? = null,
    val suppressNetToZero: Boolean? = null,
    val disableView: List<JsonElement>? = null,
    val defaultPage: String? = null,
    val displayDisclaimerInfoOnPrintPreview: Boolean? = null
)

internal data class BalanceConfigDto(
    val pageInfoText: String? = null,
    val noRecordsInfoText: String? = null,
    val currentAmountDueInfoText: String? = null,
    val accountBalanceInfoText: String? = null,
    val amountDueInfoText: String? = null,
    val dueDateInfoText: String? = null,
    val finAidInstalled: JsonElement? = null
)

internal data class TermBalancesDto(val success: Boolean? = null, val termsTotals: List<TermBalanceDto>? = null)

internal data class TermBalanceDto(
    val termCode: String? = null,
    val termDescription: String? = null,
    val termTotalBalance: BigDecimal? = null,
    val totalBalanceDisplay: String? = null,
    val cannotExpand: Boolean? = null,
    val clickable: Boolean? = null,
    val id: JsonElement? = null
)

internal data class MovementsDto(
    val result: List<MovementDto>? = null,
    val length: Int? = null,
    val totals: MovementTotalsDto? = null,
    val success: Boolean? = null
)

internal data class MovementDto(
    val detailCode: String? = null,
    val description: String? = null,
    val charge: BigDecimal? = null,
    val chargeDisplay: String? = null,
    val payment: BigDecimal? = null,
    val paymentDisplay: String? = null,
    val balance: BigDecimal? = null,
    val balanceDisplay: String? = null,
    val date: String? = null,
    val studyPathName: String? = null
)

internal data class MovementTotalsDto(
    val totalchargeDisplay: String? = null,
    val totalpaymentDisplay: String? = null,
    val totalbalanceDisplay: String? = null
)

internal data class HoldsDto(val noHolds: JsonElement? = null, val count: Int? = null)

internal data class TermsDto(
    val length: Int? = null,
    val dropDown: List<TermOptionDto>? = null,
    val success: Boolean? = null
)

internal data class TermOptionDto(
    val key: String? = null,
    val value: String? = null,
    val startDate: JsonElement? = null,
    val endDate: JsonElement? = null,
    val id: JsonElement? = null
)

internal data class CurrenciesDto(
    val result: List<CurrencyDto>? = null,
    val isMultiCurrencyEnabled: Boolean? = null,
    val hasTransactions: Boolean? = null,
    val success: Boolean? = null
)

internal data class CurrencyDto(
    val key: String? = null,
    val value: String? = null,
    val iso: String? = null,
    val isBase: Boolean? = null
)

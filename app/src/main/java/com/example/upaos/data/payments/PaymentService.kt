package com.example.upaos.data.payments

import com.example.upaos.data.model.*

/** Financial queries against the Student Self-Service contract. */
internal interface PaymentService {
    suspend fun fetchDefaultAccountSummary(currencyCode: String): MovementsDto
    suspend fun getBalanceAndConfigInfo(currencyCode: String, isOverview: Boolean): BalanceDto
    suspend fun getMyTermsAndTotals(currencyCode: String): TermBalancesDto
    suspend fun fetchAccountSummaryByTerm(currencyCode: String, termCode: String): MovementsDto
    suspend fun getHoldCount(): HoldsDto
    suspend fun getMyTerms(currencyCode: String): TermsDto
    suspend fun listForUIForStudent(): CurrenciesDto
}

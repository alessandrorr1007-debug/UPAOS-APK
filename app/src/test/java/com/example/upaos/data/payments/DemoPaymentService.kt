package com.example.upaos.data.payments

import com.example.upaos.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException

/** Offline fixture. Each synthetic period supplies its entire movement list. */
internal class DemoPaymentService(private val payload: () -> String) : PaymentService {
    private val gson = Gson()
    private val fixture by lazy {
        gson.fromJson(payload(), JsonObject::class.java) ?: throw JsonParseException("Demo de pagos vacía")
    }

    private inline fun <reified T : Any> read(name: String, source: JsonObject = fixture): T =
        gson.fromJson(source.get(name), T::class.java) ?: throw JsonParseException("Respuesta de demo incompleta")

    override suspend fun fetchDefaultAccountSummary(currencyCode: String): MovementsDto =
        fetchAccountSummaryByTerm(currencyCode, "DEMO-ALL")

    override suspend fun getBalanceAndConfigInfo(currencyCode: String, isOverview: Boolean): BalanceDto =
        read("getBalanceAndConfigInfo")

    override suspend fun getMyTermsAndTotals(currencyCode: String): TermBalancesDto = read("getMyTermsAndTotals")

    override suspend fun fetchAccountSummaryByTerm(currencyCode: String, termCode: String): MovementsDto {
        require(termCode.isNotBlank())
        return read(termCode, read<JsonObject>("fetchAccountSummaryByTerm"))
    }

    override suspend fun getHoldCount(): HoldsDto = read("getHoldCount")
    override suspend fun getMyTerms(currencyCode: String): TermsDto = read("getMyTerms")
    override suspend fun listForUIForStudent(): CurrenciesDto = read("listForUIForStudent")
}


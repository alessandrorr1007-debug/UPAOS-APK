package com.example.upaos.data.payments

import com.example.upaos.data.model.*
import com.example.upaos.data.upao.UpaoHttpClient
import com.example.upaos.data.upao.UpaoSessionExpiredException
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.Strictness
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl

internal const val ALL_TERMS = "allTerm"

internal class PaymentHttpException(val status: Int) : IOException("HTTP $status")

internal class RealPaymentService(
    client: OkHttpClient,
    private val baseUrl: HttpUrl,
    private val validateSession: () -> Unit = {}
) : PaymentService {
    constructor(session: UpaoHttpClient.AccountSession) : this(
        session.callbackClient,
        "https://ssb.upao.edu.pe/StudentSelfService/ssb/".toHttpUrl(),
        session::requireValid
    )

    private val http = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .callTimeout(35, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .cache(null)
        .build()
    private val gson = GsonBuilder().setStrictness(Strictness.STRICT).create()

    override suspend fun getBalanceAndConfigInfo(currencyCode: String, isOverview: Boolean): BalanceDto =
        get("accountSummary/getBalanceAndConfigInfo", "hasData",
            "currencyCode" to currencyCode, "isOverview" to isOverview.toString())

    override suspend fun getHoldCount(): HoldsDto = get("studentHold/getHoldCount", "count")

    override suspend fun listForUIForStudent(): CurrenciesDto =
        get("currency/listForUIForStudent", "result")

    override suspend fun getMyTermsAndTotals(currencyCode: String): TermBalancesDto =
        get("accountSummaryByTerm/getMyTermsAndTotals", "termsTotals", "currencyCode" to currencyCode)

    override suspend fun getMyTerms(currencyCode: String): TermsDto {
        val terms = linkedMapOf<String, TermOptionDto>()
        var offset = 0
        repeat(MAX_PAGES) {
            val page = get<TermsDto>("accountSummaryByTerm/getMyTerms", "dropDown",
                "currencyCode" to currencyCode, "loadNext" to "true", "max" to "5",
                "offset" to offset.toString(), "searchString" to "")
            val rows = page.dropDown.orEmpty()
            val academicRows = rows.filter { it.key != ALL_TERMS }
            val previousSize = terms.size
            rows.forEach { row ->
                val key = row.key?.takeIf(String::isNotBlank) ?: invalidResponse()
                terms.putIfAbsent(key, row)
            }
            val retrieved = terms.size - if (ALL_TERMS in terms) 1 else 0
            val length = page.length ?: invalidResponse()
            if (length < 0) invalidResponse()
            if (retrieved >= length) return page.copy(dropDown = terms.values.toList())
            if (academicRows.isEmpty() || terms.size == previousSize) invalidResponse()
            offset += academicRows.size
        }
        invalidResponse()
    }

    override suspend fun fetchDefaultAccountSummary(currencyCode: String): MovementsDto =
        movements("accountSummaryOverview/fetchDefaultAccountSummary", currencyCode)

    override suspend fun fetchAccountSummaryByTerm(currencyCode: String, termCode: String): MovementsDto {
        require(termCode.isNotBlank() && termCode != ALL_TERMS)
        // The captured request used the selected term in both parameters.
        return movements("accountSummaryByTerm/fetchAccountSummaryByTerm", currencyCode,
            "itemCode" to termCode, "termCode" to termCode, "takeFromCache" to "false")
    }

    private suspend fun movements(path: String, currencyCode: String, vararg extra: Pair<String, String>): MovementsDto {
        val rows = mutableListOf<MovementDto>()
        var totals: MovementTotalsDto? = null
        var expected: Int? = null
        repeat(MAX_PAGES) { index ->
            val page = get<MovementsDto>(path, "result",
                "currencyCode" to currencyCode, "max" to "26", "offset" to rows.size.toString(),
                "onPage" to (index + 1).toString(), "pageSize" to "26", "searchString" to "", *extra)
            val batch = page.result.orEmpty()
            val length = page.length ?: invalidResponse()
            if (length < 0 || (expected != null && length != expected)) invalidResponse()
            expected = length
            // No transaction ID is available. Identical rows can represent separate charges.
            rows.addAll(batch)
            if (totals == null) totals = page.totals
            if (rows.size == length) return page.copy(result = rows, totals = totals)
            if (batch.isEmpty() || rows.size > length) invalidResponse()
        }
        invalidResponse()
    }

    private suspend inline fun <reified T : Any> get(
        path: String,
        requiredField: String,
        vararg parameters: Pair<String, String>
    ): T {
        val url = baseUrl.newBuilder().addPathSegments(path).apply {
            parameters.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        val request = Request.Builder().url(url).get().header("Accept", "application/json")
            .cacheControl(CacheControl.FORCE_NETWORK).build()

        return exchange(request) { response ->
            if (response.isRedirect) {
                val destination = response.header("Location")?.let(url::resolve)
                if (destination != null && (
                    destination.isHttps && (destination.host in LOGIN_HOSTS ||
                        (destination.host == url.host && destination.pathSegments.any {
                            it.equals("login", ignoreCase = true)
                        }))
                )) throw UpaoSessionExpiredException()
                invalidResponse()
            }
            if (!response.isSuccessful) throw PaymentHttpException(response.code)
            val body = response.body ?: invalidResponse()
            val type = body.contentType()
            if (type?.subtype?.contains("html") == true) {
                val html = response.peekBody(64 * 1024L).string()
                val form = org.jsoup.Jsoup.parse(html).selectFirst("form input[name=nip], form input[type=password]")
                if (form != null) throw UpaoSessionExpiredException()
                invalidResponse()
            }
            val payload = response.peekBody(MAX_BODY_BYTES + 1).use { it.bytes() }
            if (payload.size > MAX_BODY_BYTES) invalidResponse()
            val json = gson.fromJson(payload.toString(Charsets.UTF_8), JsonObject::class.java) ?: invalidResponse()
            json.get("success")?.let {
                if (!it.isJsonPrimitive || !it.asJsonPrimitive.isBoolean || !it.asBoolean) invalidResponse()
            }
            val field = json.get(requiredField) ?: invalidResponse()
            when (requiredField) {
                "hasData" -> if (!field.isJsonPrimitive || !field.asJsonPrimitive.isBoolean) invalidResponse()
                "count" -> nonNegativeInteger(json, requiredField)
                else -> if (!field.isJsonArray || field.asJsonArray.any { !it.isJsonObject }) invalidResponse()
            }
            if (requiredField == "dropDown" || T::class == MovementsDto::class) {
                nonNegativeInteger(json, "length")
            }
            gson.fromJson(json, T::class.java) ?: invalidResponse()
        }
    }

    private suspend fun <T> exchange(request: Request, read: (Response) -> T): T =
        suspendCancellableCoroutine { continuation ->
            validateSession()
            val call = http.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    try {
                        validateSession()
                        if (continuation.isActive) continuation.resumeWithException(e)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        continuation.cancel(cancelled)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use {
                            validateSession()
                            val value = read(it)
                            validateSession()
                            value
                        }
                        if (continuation.isActive) continuation.resume(result)
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            })
        }

    private fun nonNegativeInteger(json: JsonObject, field: String): Int {
        val value = json.get(field) ?: invalidResponse()
        if (!value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) invalidResponse()
        return value.asString.toIntOrNull()?.takeIf { it >= 0 } ?: invalidResponse()
    }

    private fun invalidResponse(): Nothing = throw JsonParseException("Respuesta financiera inesperada")

    companion object {
        private const val MAX_PAGES = 100
        private const val MAX_BODY_BYTES = 2 * 1024 * 1024L
        private val LOGIN_HOSTS = setOf("login.upao.edu.pe", "upaosso.upao.edu.pe")
    }
}

package com.example.upaos

import com.example.upaos.data.payments.PaymentError
import com.example.upaos.data.payments.PaymentsRepository
import com.example.upaos.data.payments.RealPaymentService
import com.example.upaos.data.payments.paymentError
import com.example.upaos.data.upao.UpaoHttpClient
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test

class RealPaymentServiceTest {
    @Test
    fun loadsAllSevenContractsThroughTheRepository() = runBlocking {
        MockWebServer().use { server ->
            server.json("""{"result":[{"key":"LOCAL","iso":"PEN","isBase":true}],"isMultiCurrencyEnabled":false,"success":true}""")
            server.json("""{"hasData":true,"hasTransactions":true,"totalBalance":7.21,"currentAmountDue":0,"asOfDate":"01/01/2000","showBalance":true}""")
            server.json("""{"termsTotals":[{"termCode":"TEST-1","termTotalBalance":7.21,"clickable":false}],"success":true}""")
            server.json("""{"length":3,"dropDown":[{"key":"allTerm"},{"key":"TEST-1"},{"key":"TEST-2"}],"success":true}""")
            server.json("""{"length":3,"dropDown":[{"key":"allTerm"},{"key":"TEST-2"},{"key":"TEST-3"}],"success":true}""")
            server.json("""{"count":0,"noHolds":"No holds"}""")
            val row = """{"detailCode":"001","charge":7.210000000000001,"description":"Concepto","chargeDisplay":null,"date":null,"studyPathName":null}"""
            val rows = List(26) { row }.joinToString(",")
            server.json("""{"result":[$rows],"length":27,"totals":{"totalchargeDisplay":"PEN 187.46"},"success":true}""")
            server.json("""{"result":[$row],"length":27,"totals":{"totalchargeDisplay":"PEN 187.46"},"success":true}""")
            server.json("""{"result":[],"length":0,"success":true}""")

            val repository = PaymentsRepository(service(server))
            val overview = repository.overview()
            assertEquals("PEN", overview.currency)
            assertEquals("7.21", overview.balance)
            assertEquals("0", overview.due)
            assertTrue(overview.noAmountDue)
            assertEquals(0, overview.holds)
            assertEquals(listOf("allTerm", "TEST-1", "TEST-2", "TEST-3"), overview.periods.map { it.code })
            assertEquals("7.21", overview.periods[1].balance)
            val all = repository.movements(overview, overview.periods.first())
            assertEquals(27, all.items.size)
            assertEquals("001", all.items.first().code)
            assertEquals("7.210000000000001", all.items.first().amounts.charge)
            assertNull(all.items.first().amounts.payment)
            assertNull(all.items.first().date)
            assertEquals("PEN 187.46", all.totals?.charge)
            assertTrue(repository.movements(overview, overview.periods[1]).items.isEmpty())

            server.request("currency/listForUIForStudent")
            server.request("accountSummary/getBalanceAndConfigInfo", "currencyCode" to "LOCAL", "isOverview" to "true")
            server.request("accountSummaryByTerm/getMyTermsAndTotals", "currencyCode" to "")
            server.request("accountSummaryByTerm/getMyTerms", "currencyCode" to "", "loadNext" to "true", "max" to "5", "offset" to "0")
            server.request("accountSummaryByTerm/getMyTerms", "offset" to "2")
            server.request("studentHold/getHoldCount")
            server.request("accountSummaryOverview/fetchDefaultAccountSummary", "currencyCode" to "LOCAL",
                "onPage" to "1", "offset" to "0", "max" to "26", "pageSize" to "26", "searchString" to "")
            server.request("accountSummaryOverview/fetchDefaultAccountSummary", "offset" to "26", "onPage" to "2")
            val detailRequest = server.request("accountSummaryByTerm/fetchAccountSummaryByTerm", "currencyCode" to "",
                "itemCode" to "TEST-1", "termCode" to "TEST-1", "takeFromCache" to "false")
            assertEquals("", detailRequest.requestUrl!!.queryParameter("searchString"))
        }
    }

    @Test
    fun preservesBigDecimalAndEncodesQueryWithoutAddingParameters() = runBlocking {
        MockWebServer().use { server ->
            server.json("""{"hasData":true,"totalBalance":0.000000000000001,"currentAmountDueDisplay":null}""")
            val value = service(server).getBalanceAndConfigInfo("A & # + /", true)
            assertEquals(BigDecimal("0.000000000000001"), value.totalBalance)
            assertNull(value.currentAmountDueDisplay)
            val request = server.request("accountSummary/getBalanceAndConfigInfo", "currencyCode" to "A & # + /")
            assertEquals(setOf("currencyCode", "isOverview"), request.requestUrl!!.queryParameterNames)
        }
    }

    @Test
    fun distinguishesAuthenticationTransportAndProtocolErrors() = runBlocking {
        MockWebServer().use { server ->
            val client = OkHttpClient.Builder().readTimeout(150, TimeUnit.MILLISECONDS).build()
            val service = service(server, client)
            val cases = listOf(
                MockResponse().setResponseCode(401) to PaymentError.EXPIRED,
                MockResponse().setResponseCode(403) to PaymentError.UNAUTHORIZED,
                MockResponse().setResponseCode(302).setHeader("Location", "https://login.upao.edu.pe/cas/login") to PaymentError.EXPIRED,
                MockResponse().setResponseCode(302).setHeader("Location", "https://example.com/elsewhere") to PaymentError.RESPONSE,
                MockResponse().setHeader("Content-Type", "text/html").setBody("<html>Maintenance</html>") to PaymentError.RESPONSE,
                MockResponse().setHeader("Content-Type", "text/html").setBody("<form><input name=nip></form>") to PaymentError.EXPIRED,
                MockResponse().setBody("""{"count":0""") to PaymentError.RESPONSE,
                MockResponse().setBody("""{"count":0,"success":false}""") to PaymentError.RESPONSE,
                MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE) to PaymentError.CONNECTION
            )
            for ((response, expected) in cases) {
                server.enqueue(response)
                expect(expected) { service.getHoldCount() }
            }
            assertEquals(cases.size, server.requestCount)
        }
        val closed = MockWebServer()
        val service = service(closed)
        closed.shutdown()
        expect(PaymentError.CONNECTION) { service.getHoldCount() }
    }

    @Test
    fun renewsOnlyOnceAfterAnExpiredInitialRequest() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(MockResponse().setResponseCode(401))
            var renewals = 0
            val repository = PaymentsRepository(service(server)) {
                renewals++
                true
            }
            expect(PaymentError.EXPIRED) { repository.overview() }
            assertEquals(1, renewals)
            assertEquals(2, server.requestCount)
        }
    }

    @Test
    fun renewsWhenDetailExpiresAfterAnEarlierSuccessfulOverview() = runBlocking {
        MockWebServer().use { server ->
            server.json("""{"result":[{"key":"SOL","iso":"PEN","isBase":true}]}""")
            server.json("""{"hasData":true,"totalBalance":-1.125,"currentAmountDue":0}""")
            server.json("""{"termsTotals":[{"termCode":"0001","termTotalBalance":0}]}""")
            server.json("""{"length":1,"dropDown":[{"key":"0001"}]}""")
            server.json("""{"count":0}""")
            server.enqueue(MockResponse().setResponseCode(401))
            server.json("""{"result":[{"charge":0,"payment":0,"balanceDisplay":"PEN\u00a00.00"}],"length":1}""")
            var renewals = 0
            val repository = PaymentsRepository(service(server)) { renewals++; true }
            val overview = repository.overview()
            assertEquals("-1.125", overview.balance)
            assertEquals("0", overview.periods.single().balance)
            val movement = repository.movements(overview, overview.periods.single()).items.single()
            assertEquals("0", movement.amounts.charge)
            assertEquals("0", movement.amounts.payment)
            assertEquals("PEN 0.00", movement.amounts.balance)
            assertEquals(1, renewals)
            assertEquals(7, server.requestCount)
        }
    }

    @Test
    fun rejectsInvalidShapesAndUntrustedLoginRedirects() = runBlocking {
        MockWebServer().use { server ->
            val service = service(server)
            for (body in listOf("", """{"count":-1}""", """{"count":"0"}""", """{"count":0,"success":"true"}""")) {
                server.json(body)
                expect(PaymentError.RESPONSE) { service.getHoldCount() }
            }
            server.json("""{"result":[null],"length":1}""")
            expect(PaymentError.RESPONSE) { service.fetchDefaultAccountSummary("SOL") }
            server.json("""{"result":[],"length":"0"}""")
            expect(PaymentError.RESPONSE) { service.fetchDefaultAccountSummary("SOL") }
            server.json("""{"hasData":"false"}""")
            expect(PaymentError.RESPONSE) { service.getBalanceAndConfigInfo("SOL", true) }
            server.json("""{"count":0,"padding":"${"x".repeat(2 * 1024 * 1024)}"}""")
            expect(PaymentError.RESPONSE) { service.getHoldCount() }
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "https://example.com/login"))
            expect(PaymentError.RESPONSE) { service.getHoldCount() }
            for ((status, expected) in mapOf(400 to PaymentError.RESPONSE, 404 to PaymentError.RESPONSE,
                429 to PaymentError.UNAVAILABLE, 500 to PaymentError.SERVER, 502 to PaymentError.UNAVAILABLE,
                503 to PaymentError.UNAVAILABLE)) {
                server.enqueue(MockResponse().setResponseCode(status))
                expect(expected) { service.getHoldCount() }
            }
        }
    }

    @Test
    fun failsIncompletePagesInsteadOfDisplayingPartialTotals() = runBlocking {
        MockWebServer().use { server ->
            server.json("""{"result":[{"detailCode":"TEST"}],"length":3}""")
            server.json("""{"result":[],"length":3}""")
            expect(PaymentError.RESPONSE) { service(server).fetchDefaultAccountSummary("LOCAL") }
            server.json("""{"length":2,"dropDown":[{"key":"allTerm"},{"key":"TEST"}]}""")
            server.json("""{"length":2,"dropDown":[{"key":"allTerm"},{"key":"TEST"}]}""")
            expect(PaymentError.RESPONSE) { service(server).getMyTerms("") }
            assertEquals(4, server.requestCount)
        }
    }

    @Test
    fun accountSwitchAndLogoutCancelRequestsAndIsolateCookies() = runBlocking {
        MockWebServer().use { server ->
            val accountA = UpaoHttpClient.selectAccount("test-A")
            try {
                server.enqueue(MockResponse().setHeader("Set-Cookie", "fixture=A; Path=/").setBody("""{"count":0}"""))
                val serviceA = RealPaymentService(accountA.callbackClient, server.url("/"), accountA::requireValid)
                serviceA.getHoldCount()
                assertNull(server.request("studentHold/getHoldCount").getHeader("Cookie"))
                server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
                val old = async(Dispatchers.IO) { serviceA.getHoldCount() }
                assertEquals("fixture=A", server.request("studentHold/getHoldCount").getHeader("Cookie"))

                val accountB = UpaoHttpClient.selectAccount("test-B")
                old.join()
                assertTrue(old.isCancelled)
                assertTrue(accountA.cookieJar.loadForRequest(server.url("/")).isEmpty())
                val serviceB = RealPaymentService(accountB.callbackClient, server.url("/"), accountB::requireValid)
                server.json("""{"count":0}""")
                serviceB.getHoldCount()
                assertNull(server.request("studentHold/getHoldCount").getHeader("Cookie"))

                server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
                val pending = async(Dispatchers.IO) { serviceB.getHoldCount() }
                server.request("studentHold/getHoldCount")
                UpaoHttpClient.clearAccount("test-B")
                pending.join()
                assertTrue(pending.isCancelled)
            } finally {
                UpaoHttpClient.clearAccount("test-A")
                UpaoHttpClient.clearAccount("test-B")
            }
        }
    }

    @Test
    fun cancellingCoroutineCancelsHttpCall() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val cancelled = kotlinx.coroutines.CompletableDeferred<Unit>()
            val http = OkHttpClient.Builder().eventListener(object : okhttp3.EventListener() {
                override fun canceled(call: okhttp3.Call) {
                    cancelled.complete(Unit)
                }
            }).build()
            val query = async(Dispatchers.IO) { service(server, http).getHoldCount() }
            server.request("studentHold/getHoldCount")
            query.cancelAndJoin()
            kotlinx.coroutines.withTimeout(3_000) { cancelled.await() }
        }
    }

    private fun service(server: MockWebServer, client: OkHttpClient = OkHttpClient()) =
        RealPaymentService(client, server.url("/StudentSelfService/ssb/"))

    private fun MockWebServer.json(body: String) =
        enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body))

    private fun MockWebServer.request(path: String, vararg query: Pair<String, String>): RecordedRequest {
        val request = requireNotNull(takeRequest(5, TimeUnit.SECONDS))
        assertEquals("GET", request.method)
        assertTrue(request.requestUrl!!.encodedPath.endsWith("/$path"))
        query.forEach { (key, value) -> assertEquals(value, request.requestUrl!!.queryParameter(key)) }
        return request
    }

    private suspend fun expect(expected: PaymentError, block: suspend () -> Unit) {
        try {
            block()
            fail("Expected $expected")
        } catch (e: Exception) {
            assertEquals(expected, paymentError(e))
        }
    }
}

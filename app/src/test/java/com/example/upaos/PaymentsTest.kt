package com.example.upaos

import com.example.upaos.data.model.BalanceDto
import com.example.upaos.data.model.MovementsDto
import com.example.upaos.data.payments.DemoPaymentService
import com.example.upaos.data.payments.PaymentError
import com.example.upaos.data.payments.PaymentService
import com.example.upaos.data.payments.PaymentsRepository
import com.example.upaos.data.payments.paymentError
import com.example.upaos.ui.payments.PaymentsViewModel
import com.google.gson.JsonParseException
import java.io.IOException
import java.math.BigDecimal
import java.net.SocketTimeoutException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentsTest {
    private val demo = DemoPaymentService { requireNotNull(javaClass.getResource("/payments_demo.json")).readText() }

    @Test
    fun mapsAllSixFixturesAndPreservesMovementSemantics() = runTest {
        val repository = PaymentsRepository(demo, StandardTestDispatcher(testScheduler))
        val overview = repository.overview()
        assertEquals("PEN", overview.currency)
        assertEquals("S/ 350.00", overview.balance)
        assertEquals("S/ 150.00", overview.due)
        assertEquals("15/09/2026", overview.date)
        assertEquals(0, overview.holds)
        assertEquals("Todos los periodos", overview.periods.first().label)
        val period = overview.periods.single { it.label == "2026-II" }
        assertEquals("S/ 350.00", period.balance)

        val detail = repository.movements(overview, period)
        assertEquals("DEMO-CARGO", detail.items.first().code)
        assertEquals("S/ 500.00", detail.items.first().amounts.charge)
        assertNull(detail.items.first().amounts.payment)
        assertEquals("S/ 150.00", detail.items.last().amounts.payment)
        assertNull(detail.items.last().date)
        assertEquals("S/ 350.00", detail.totals?.balance)
        assertTrue(repository.movements(overview, overview.periods.last()).items.isEmpty())
    }

    @Test
    fun classifiesNetworkAndHttpErrorsWithoutUsingResponseBodies() {
        assertEquals(PaymentError.CONNECTION, paymentError(IOException()))
        assertEquals(PaymentError.RESPONSE, paymentError(JsonParseException("invalid")))
        for ((status, error) in mapOf(
            401 to PaymentError.EXPIRED, 403 to PaymentError.UNAUTHORIZED,
            500 to PaymentError.SERVER, 503 to PaymentError.UNAVAILABLE, 429 to PaymentError.UNAVAILABLE
        )) {
            assertEquals(error, paymentError(httpError(status)))
        }
    }

    @Test
    fun changingPeriodCancelsPreviousQueryAndClearingDropsTheAccountState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val requests = mutableListOf<String>()
        var cancellations = 0
        val service = object : PaymentService by demo {
            override suspend fun fetchAccountSummaryByTerm(currencyCode: String, termCode: String): MovementsDto {
                assertEquals("", currencyCode)
                requests += termCode
                if (termCode == "DEMO-ALL") {
                    try { awaitCancellation() } finally { cancellations++ }
                }
                return demo.fetchAccountSummaryByTerm(currencyCode, termCode)
            }
        }
        Dispatchers.setMain(dispatcher)
        try {
            val model = PaymentsViewModel(PaymentsRepository(service, dispatcher))
            model.refresh()
            model.refresh()
            runCurrent()
            val empty = model.state.value.overview!!.periods.last()
            model.selectPeriod(empty)
            model.selectPeriod(empty)
            advanceUntilIdle()
            assertEquals(listOf("DEMO-ALL", empty.code), requests)
            assertEquals(1, cancellations)
            assertEquals(empty, model.state.value.period)
            assertTrue(model.state.value.movements!!.items.isEmpty())
            model.clear()
            model.refresh()
            runCurrent()
            model.clear()
            advanceUntilIdle()
            assertEquals(2, cancellations)
            assertNull(model.state.value.overview)
            assertNull(model.state.value.movements)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun expiredSessionClearsOverviewAndMovements() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val service = object : PaymentService by demo {
            override suspend fun fetchAccountSummaryByTerm(currencyCode: String, termCode: String): MovementsDto {
                throw httpError(401)
            }
        }
        Dispatchers.setMain(dispatcher)
        try {
            val model = PaymentsViewModel(PaymentsRepository(service, dispatcher))
            model.refresh()
            advanceUntilIdle()
            assertEquals(PaymentError.EXPIRED, model.state.value.error)
            assertFalse(model.state.value.loading)
            assertNull(model.state.value.overview)
            assertNull(model.state.value.movements)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun emptySuccessfulListsProduceAnEmptyOverview() = runTest {
        val service = DemoPaymentService {
            """
            {
              "listForUIForStudent": {"success": true, "result": []},
              "getBalanceAndConfigInfo": {"hasData": false},
              "getMyTermsAndTotals": {"success": true, "termsTotals": []},
              "getMyTerms": {"success": true, "dropDown": []},
              "getHoldCount": {}
            }
            """.trimIndent()
        }
        val overview = PaymentsRepository(service, StandardTestDispatcher(testScheduler)).overview()
        assertTrue(overview.periods.isEmpty())
        assertNull(overview.currency)
        assertNull(overview.balance)
        assertNull(overview.due)
        assertNull(overview.holds)
        assertFalse(overview.noAmountDue)
    }

    @Test
    fun onlyVisibleNumericZeroIsShownAsNoDebt() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val service = object : PaymentService by demo {
            var balance = BalanceDto(hasData = true, currentAmountDue = BigDecimal.ZERO)
            override suspend fun getBalanceAndConfigInfo(currencyCode: String, isOverview: Boolean) = balance
        }
        val repository = PaymentsRepository(service, dispatcher)
        assertTrue(repository.overview().noAmountDue)
        service.balance = service.balance.copy(showCurrentAmountDue = false)
        assertFalse(repository.overview().noAmountDue)
        service.balance = service.balance.copy(showCurrentAmountDue = true, currentAmountDue = null)
        assertFalse(repository.overview().noAmountDue)
    }

    @Test
    fun invalidFixtureBodiesAndMissingOperationsFailWithResponseError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        for (body in listOf("", "{}", """{"listForUIForStudent": {}}""", "<html>Sign in</html>")) {
            val repository = PaymentsRepository(DemoPaymentService { body }, dispatcher)
            try {
                repository.overview()
                fail("Invalid fixture should not produce an overview")
            } catch (error: JsonParseException) {
                assertEquals(PaymentError.RESPONSE, paymentError(error))
            }
        }
    }

    @Test
    fun exposesForbiddenServerUnavailableAndTimeoutErrorsAndCanRefreshAfterFailure() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            for ((failure, expected) in listOf(
                httpError(403) to PaymentError.UNAUTHORIZED,
                httpError(500) to PaymentError.SERVER,
                httpError(503) to PaymentError.UNAVAILABLE,
                SocketTimeoutException() to PaymentError.CONNECTION
            )) {
                var attempts = 0
                val service = object : PaymentService by demo {
                    override suspend fun fetchAccountSummaryByTerm(currencyCode: String, termCode: String): MovementsDto {
                        if (attempts++ == 0) throw failure
                        return demo.fetchAccountSummaryByTerm(currencyCode, termCode)
                    }
                }
                val model = PaymentsViewModel(PaymentsRepository(service, dispatcher))
                model.refresh()
                advanceUntilIdle()
                val failed = model.state.value
                assertEquals(expected, failed.error)
                assertFalse(failed.loading)
                assertNull(failed.movements)
                if (expected == PaymentError.UNAUTHORIZED) {
                    assertNull(failed.overview)
                    assertNull(failed.period)
                } else {
                    assertNotNull(failed.overview)
                }

                model.refresh()
                advanceUntilIdle()
                assertNull(model.state.value.error)
                assertEquals(2, model.state.value.movements?.items?.size)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun lateResultFromClearedModelCannotPublishIntoAnotherSession() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val response = CompletableDeferred<MovementsDto>()
        val service = object : PaymentService by demo {
            override suspend fun fetchAccountSummaryByTerm(currencyCode: String, termCode: String): MovementsDto =
                withContext(NonCancellable) { response.await() }
        }
        Dispatchers.setMain(dispatcher)
        try {
            val previous = PaymentsViewModel(PaymentsRepository(service, dispatcher))
            previous.refresh()
            runCurrent()
            assertNotNull(previous.state.value.overview)
            previous.clear()

            val current = PaymentsViewModel(PaymentsRepository(demo, dispatcher))
            current.refresh()
            advanceUntilIdle()
            val currentState = current.state.value
            assertEquals(2, currentState.movements?.items?.size)

            response.complete(MovementsDto(success = true, result = emptyList()))
            advanceUntilIdle()
            assertNull(previous.state.value.overview)
            assertNull(previous.state.value.movements)
            assertEquals(currentState, current.state.value)
        } finally {
            response.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun invalidatedAccountCannotPublishCompletedMovement() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val response = CompletableDeferred<MovementsDto>()
        var active = true
        val service = object : PaymentService by demo {
            override suspend fun fetchAccountSummaryByTerm(currencyCode: String, termCode: String) = response.await()
        }
        Dispatchers.setMain(dispatcher)
        try {
            val previous = PaymentsViewModel(PaymentsRepository(service, dispatcher)) {
                if (!active) throw kotlinx.coroutines.CancellationException()
            }
            previous.refresh()
            runCurrent()
            active = false
            val current = PaymentsViewModel(PaymentsRepository(demo, dispatcher))
            current.refresh()
            response.complete(MovementsDto(result = emptyList(), success = true))
            advanceUntilIdle()
            assertNull(previous.state.value.movements)
            assertEquals(2, current.state.value.movements?.items?.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun httpError(status: Int): HttpException = HttpException(Response.error<Unit>(status, "".toResponseBody()))
}

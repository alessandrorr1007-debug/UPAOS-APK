package com.example.upaos

import com.example.upaos.ui.theme.UPAOSTheme
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.upaos.data.payments.AccountOverview
import com.example.upaos.data.payments.MovementAmounts
import com.example.upaos.data.payments.PaymentError
import com.example.upaos.data.payments.PaymentMovement
import com.example.upaos.data.payments.PaymentPeriod
import com.example.upaos.data.payments.PeriodMovements
import com.example.upaos.ui.payments.PaymentsContent
import com.example.upaos.ui.payments.PaymentsHome
import com.example.upaos.ui.payments.PaymentsTopBar
import com.example.upaos.ui.payments.PaymentsState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PaymentsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun loadingAndRetryUseTheCurrentState() {
        val state = mutableStateOf(PaymentsState())
        var retried = false
        compose.setContent {
            UPAOSTheme(darkTheme = false) { PaymentsContent(state.value, {}, { retried = true }, {}) }
        }
        compose.onNodeWithText("Consultando estado de cuenta…").assertIsDisplayed()
        compose.runOnIdle {
            state.value = PaymentsState(loading = false, error = PaymentError.EXPIRED)
        }
        compose.onNodeWithText(PaymentError.EXPIRED.message).assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle { assertEquals(true, retried) }
    }

    @Test
    fun displaysZeroAndSelectsAnotherPeriod() {
        val first = PaymentPeriod("allTerm", "Todos los periodos", null, true)
        val second = PaymentPeriod("TEST-2", "Periodo dos", null, true)
        val overview = AccountOverview("PEN 0.00", "PEN 0.00", null, "PEN", 0,
            listOf(first, second), "LOCAL", true, true, true)
        val state = mutableStateOf(PaymentsState(overview, first, PeriodMovements(emptyList(), null), false))
        compose.setContent {
            UPAOSTheme(darkTheme = true) {
                PaymentsContent(state.value, { period ->
                    val movement = PaymentMovement("TEST", "Concepto de prueba", null,
                        MovementAmounts("PEN 1.00", null, null))
                    state.value = state.value.copy(period = period, movements = PeriodMovements(listOf(movement), null))
                }, {}, {})
            }
        }
        compose.onNodeWithText("Saldo total").assertIsDisplayed()
        compose.onAllNodesWithText("PEN 0.00")[0].assertIsDisplayed()
        compose.onNodeWithText("Resumen de cuenta").assertIsSelected()
        compose.onNodeWithText("Periodo académico").performClick()
        compose.onNodeWithText("Periodo académico").assertIsSelected()
        compose.onNodeWithText(first.label).assertIsDisplayed()
        compose.onNodeWithText(second.label).performClick()
        compose.onNodeWithText("Concepto de prueba").assertIsDisplayed()
    }

    @Test
    fun entryOpensAccountAndRefreshHasAnAccessibleAction() {
        val opened = mutableStateOf(false)
        var refreshed = false
        compose.setContent {
            UPAOSTheme(darkTheme = false) {
                if (opened.value) PaymentsTopBar({ opened.value = false }, { refreshed = true }, false)
                else PaymentsHome { opened.value = true }
            }
        }
        compose.onNodeWithText("Próximamente").assertIsDisplayed()
        compose.onNodeWithText("Estado de cuenta").performClick()
        compose.onNodeWithContentDescription("Actualizar estado de cuenta").performClick()
        compose.runOnIdle { assertEquals(true, refreshed) }
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Pagos").assertIsDisplayed()
    }
}

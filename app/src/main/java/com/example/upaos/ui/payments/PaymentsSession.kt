package com.example.upaos.ui.payments

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.upaos.data.local.TokenManager
import com.example.upaos.data.payments.RealPaymentService
import com.example.upaos.data.upao.UpaoAuth
import com.example.upaos.data.upao.UpaoHttpClient
import kotlinx.coroutines.CancellationException
import com.example.upaos.data.payments.PaymentsRepository

@Composable
internal fun rememberPaymentsViewModel(user: String, session: String): PaymentsViewModel? {
    val context = LocalContext.current.applicationContext
    val factory = remember(context, user, session) {
        val tokens = TokenManager(context)
        val account = try {
            UpaoHttpClient.forAccount(user)
        } catch (_: CancellationException) {
            return@remember null
        }
        val repository = PaymentsRepository(RealPaymentService(account)) {
            account.requireValid()
            if (tokens.getSavedUser() != user || tokens.getSessionId() != session) {
                throw CancellationException("La cuenta cambió")
            }
            val password = tokens.getSavedPass() ?: return@PaymentsRepository false
            account.requireValid()
            UpaoAuth.renewSession(account, password).success
        }
        PaymentsViewModel.factory(repository, account::requireValid)
    }
    return viewModel(key = "payments_$session", factory = factory ?: return null)
}

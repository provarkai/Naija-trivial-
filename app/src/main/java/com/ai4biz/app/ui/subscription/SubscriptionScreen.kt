package com.ai4biz.app.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.ai4biz.app.billing.PlanId
import com.ai4biz.app.ui.LocalAppContainer
import com.ai4biz.app.util.findActivity
import kotlinx.coroutines.launch

private data class Plan(val id: PlanId?, val name: String, val blurb: String)

private val plans = listOf(
    Plan(null, "Free", "5 free generations a day, with ads."),
    Plan(PlanId.MONTHLY, "Monthly", "Unlimited generations across all MVP tools, no ads."),
    Plan(PlanId.ANNUAL, "Annual", "Same as Monthly, priced for a full year."),
    Plan(PlanId.LIFETIME, "Lifetime", "One-time payment, all future MVP tool updates included.")
)

private val comingSoon = listOf("AI credit packs", "Team plans", "White-label edition")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(navController: NavHostController) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isPremium by container.billingManager.isPremium.collectAsStateWithLifecycle()
    val activePlan by container.billingManager.activePlan.collectAsStateWithLifecycle()

    // Purchases can complete/update while this screen isn't visible (e.g.
    // Play's own payment sheet); re-check on return.
    LaunchedEffect(Unit) {
        container.billingManager.refreshPurchases()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Subscription") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(plans) { plan ->
                val isCurrentPlan = (plan.id == null && !isPremium) || (plan.id != null && plan.id == activePlan)
                val price = plan.id?.let { container.billingManager.priceFor(it) }

                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = plan.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when {
                                plan.id == null -> "₦0"
                                price != null -> price
                                else -> "Price unavailable"
                            },
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = plan.blurb,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        when {
                            isCurrentPlan -> Text(
                                text = "Current plan",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            plan.id == null -> { /* nothing to buy for Free */ }
                            else -> Button(
                                onClick = {
                                    if (activity == null) {
                                        scope.launch { snackbarHostState.showSnackbar("Can't start checkout right now") }
                                    } else if (price == null) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "This plan isn't available yet -- check back soon"
                                            )
                                        }
                                    } else {
                                        container.billingManager.launchPurchase(activity, plan.id)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Choose ${plan.name}")
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Coming soon",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    comingSoon.forEach { label ->
                        AssistChip(onClick = {}, label = { Text(label) })
                    }
                }
            }
        }
    }
}

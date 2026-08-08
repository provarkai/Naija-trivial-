package com.ai4biz.app.ui.businesssetup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ai4biz.app.navigation.Routes
import com.ai4biz.app.ui.LocalAppContainer
import com.ai4biz.app.ui.SimpleViewModelFactory
import com.ai4biz.app.ui.businesssetup.steps.BrandPersonalityStep
import com.ai4biz.app.ui.businesssetup.steps.BusinessBasicsStep
import com.ai4biz.app.ui.businesssetup.steps.BusinessGoalsStep
import com.ai4biz.app.ui.businesssetup.steps.ContactInfoStep
import com.ai4biz.app.ui.businesssetup.steps.IndustryStep
import com.ai4biz.app.ui.businesssetup.steps.LocationStep
import com.ai4biz.app.ui.businesssetup.steps.ProductsStep
import com.ai4biz.app.ui.businesssetup.steps.TargetCustomersStep
import kotlinx.coroutines.launch

private val STEP_TITLES = listOf(
    "Business basics", "Industry", "Products & services", "Customers",
    "Location", "Brand", "Goals", "Contact info"
)
private const val TOTAL_STEPS = 8

/**
 * The Business Setup wizard (Phase 2 Sprint 2, see docs/PHASE2_ARCHITECTURE.md).
 * One route, one screen -- [currentStep][BusinessSetupViewModel.currentStep]
 * is internal state, not separate nav routes, so wizard state survives
 * Next/Back without per-route ViewModel scoping.
 *
 * [onFinished] defaults to "pop back if possible, else go Home" -- this
 * single static default correctly handles both entry points without the
 * NavHost needing two different route registrations: a fresh sign-in
 * leaves the back stack as just [BUSINESS_SETUP] (Auth already popped
 * itself), so popBackStack() fails and falls through to Home; re-entry
 * from Profile leaves Profile on the stack below, so popBackStack()
 * succeeds and returns there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(
    navController: NavHostController,
    onFinished: () -> Unit = {
        val popped = navController.popBackStack()
        if (!popped) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.BUSINESS_SETUP) { inclusive = true }
            }
        }
    }
) {
    val container = LocalAppContainer.current
    val viewModel: BusinessSetupViewModel = viewModel(
        factory = SimpleViewModelFactory {
            BusinessSetupViewModel(
                container.workspaceRepository,
                container.businessProfileRepository,
                container.brandSettingsRepository,
                container.productServiceRepository,
                container.businessGoalRepository,
                container.onboardingRepository
            )
        }
    )
    val isLoadingExisting by viewModel.isLoadingExisting.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = viewModel.currentStep > 0) { viewModel.previousStep() }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(STEP_TITLES[viewModel.currentStep]) },
                    actions = {
                        TextButton(onClick = { scope.launch { viewModel.skip(); onFinished() } }) {
                            Text("Skip for now")
                        }
                    }
                )
                LinearProgressIndicator(
                    progress = { (viewModel.currentStep + 1) / TOTAL_STEPS.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (viewModel.currentStep > 0) {
                    OutlinedButton(onClick = { viewModel.previousStep() }, modifier = Modifier.weight(1f)) {
                        Text("Back")
                    }
                }
                Button(
                    enabled = viewModel.canProceedFromCurrentStep && !isSaving,
                    onClick = {
                        if (viewModel.currentStep == TOTAL_STEPS - 1) {
                            scope.launch { viewModel.finish(); onFinished() }
                        } else {
                            viewModel.nextStep()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (viewModel.currentStep == TOTAL_STEPS - 1) "Finish" else "Next")
                }
            }
        }
    ) { padding ->
        if (isLoadingExisting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AnimatedContent(
                targetState = viewModel.currentStep,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                label = "business_setup_step"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    when (step) {
                        0 -> BusinessBasicsStep(viewModel)
                        1 -> IndustryStep(viewModel)
                        2 -> ProductsStep(viewModel)
                        3 -> TargetCustomersStep(viewModel)
                        4 -> LocationStep(viewModel)
                        5 -> BrandPersonalityStep(viewModel)
                        6 -> BusinessGoalsStep(viewModel)
                        else -> ContactInfoStep(viewModel)
                    }
                }
            }
        }
    }
}

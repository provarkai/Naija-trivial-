package com.ai4biz.app.ui.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ai4biz.app.ads.findActivity
import com.ai4biz.app.data.repository.UsageState
import com.ai4biz.app.model.ToolType
import com.ai4biz.app.navigation.Routes
import com.ai4biz.app.ui.LocalAppContainer
import com.ai4biz.app.ui.SimpleViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(navController: NavHostController, toolId: String) {
    val tool = ToolType.fromId(toolId)
    if (tool == null) {
        Text("Unknown tool: $toolId", modifier = Modifier.padding(16.dp))
        return
    }

    val container = LocalAppContainer.current
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()

    val viewModel: GeneratorViewModel = viewModel(
        factory = SimpleViewModelFactory {
            GeneratorViewModel(tool, container.aiGeneratorService, container.documentRepository)
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val usageState by container.usageRepository.usageState.collectAsStateWithLifecycle(initialValue = UsageState())
    val inputs = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is GeneratorUiState.Success) {
            container.usageRepository.recordGeneration()

            fun proceed() {
                navController.navigate(Routes.result(state.documentId)) {
                    popUpTo(Routes.generator(toolId)) { inclusive = true }
                }
            }

            val shouldShowAd = container.interstitialAdManager.registerGenerationAndShouldShow()
            if (shouldShowAd && activity != null) {
                container.interstitialAdManager.showIfReady(activity) { proceed() }
            } else {
                proceed()
            }
        }
    }

    val isLoading = uiState is GeneratorUiState.Loading
    val requiredFilled = tool.fields.filter { it.required }.all { !inputs[it.key].isNullOrBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tool.title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = tool.description, style = MaterialTheme.typography.bodyMedium)

            tool.fields.forEach { field ->
                OutlinedTextField(
                    value = inputs[field.key].orEmpty(),
                    onValueChange = { inputs[field.key] = it },
                    label = { Text(field.label + if (!field.required) " (optional)" else "") },
                    placeholder = { Text(field.placeholder) },
                    singleLine = !field.multiline,
                    minLines = if (field.multiline) 3 else 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState is GeneratorUiState.Error) {
                Text(
                    text = (uiState as GeneratorUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (usageState.canGenerate) {
                val bonusSuffix = if (usageState.bonusGenerations > 0) " (+${usageState.bonusGenerations} bonus)" else ""
                Text(
                    text = "${usageState.remainingFree} free generation${if (usageState.remainingFree == 1) "" else "s"} left today$bonusSuffix",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "You've used today's free generations",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Watch a short ad for one more, or upgrade for unlimited generations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                if (activity != null) {
                                    container.rewardedAdManager.showIfReady(
                                        activity = activity,
                                        onEarned = {
                                            scope.launch { container.usageRepository.addBonusGeneration() }
                                        },
                                        onUnavailable = { /* ad not ready yet; button stays visible to retry */ }
                                    )
                                }
                            }) {
                                Text("Watch ad for +1")
                            }
                            Button(onClick = { navController.navigate(Routes.SUBSCRIPTION) }) {
                                Text("View plans")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.generate(inputs.toMap()) },
                enabled = requiredFilled && !isLoading && usageState.canGenerate,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(if (isLoading) "Generating…" else "Generate")
            }
        }
    }
}

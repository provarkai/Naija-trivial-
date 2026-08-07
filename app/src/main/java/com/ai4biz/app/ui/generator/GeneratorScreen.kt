package com.ai4biz.app.ui.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ai4biz.app.model.ToolType
import com.ai4biz.app.navigation.Routes
import com.ai4biz.app.ui.LocalAppContainer
import com.ai4biz.app.ui.SimpleViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(navController: NavHostController, toolId: String) {
    val tool = ToolType.fromId(toolId)
    if (tool == null) {
        Text("Unknown tool: $toolId", modifier = Modifier.padding(16.dp))
        return
    }

    val container = LocalAppContainer.current
    val viewModel: GeneratorViewModel = viewModel(
        factory = SimpleViewModelFactory {
            GeneratorViewModel(tool, container.aiGeneratorService, container.documentRepository)
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val inputs = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is GeneratorUiState.Success) {
            navController.navigate(Routes.result(state.documentId)) {
                popUpTo(Routes.generator(toolId)) { inclusive = true }
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

            Button(
                onClick = { viewModel.generate(inputs.toMap()) },
                enabled = requiredFilled && !isLoading,
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

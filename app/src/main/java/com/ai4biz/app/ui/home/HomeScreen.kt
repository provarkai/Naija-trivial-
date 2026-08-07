package com.ai4biz.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ai4biz.app.model.ToolType
import com.ai4biz.app.navigation.Routes
import com.ai4biz.app.ui.LocalAppContainer
import com.ai4biz.app.ui.SimpleViewModelFactory

private fun iconFor(tool: ToolType): ImageVector = when (tool) {
    ToolType.BUSINESS_PLAN -> Icons.AutoMirrored.Filled.Assignment
    ToolType.PROPOSAL -> Icons.Filled.Description
    ToolType.INVOICE_RECEIPT -> Icons.Filled.Receipt
    ToolType.SOCIAL_MEDIA_CONTENT -> Icons.Filled.Campaign
    ToolType.WHATSAPP_REPLY -> Icons.AutoMirrored.Filled.Chat
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(
        factory = SimpleViewModelFactory { HomeViewModel(container.authRepository) }
    )
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Edge AI") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.HISTORY) }) {
                        Icon(Icons.Filled.History, contentDescription = "History")
                    }
                    IconButton(onClick = { navController.navigate(Routes.SUBSCRIPTION) }) {
                        Icon(Icons.Filled.WorkspacePremium, contentDescription = "Subscription")
                    }
                    IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                val greetingName = authState.displayName.ifBlank { "there" }
                Text(
                    text = "Hi, $greetingName 👋",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Pick a tool to generate your next business document.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
            }
            items(ToolType.mvpTools) { tool ->
                ToolCard(
                    tool = tool,
                    onClick = { navController.navigate(Routes.generator(tool.id)) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ToolCard(tool: ToolType, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = iconFor(tool),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(text = tool.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

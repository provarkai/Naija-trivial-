package com.ai4biz.app.ui.workspace

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ai4biz.app.model.Customer
import com.ai4biz.app.model.GeneratedDocument
import com.ai4biz.app.model.ProductService
import com.ai4biz.app.model.ProductServiceType
import com.ai4biz.app.navigation.Routes
import com.ai4biz.app.ui.LocalAppContainer
import com.ai4biz.app.ui.SimpleViewModelFactory
import java.text.DateFormat
import java.util.Date

private val TAB_TITLES = listOf("Documents", "Products", "Customers", "Business Profile")

/**
 * The Workspace screen (Phase 2 Sprint 3, see docs/PHASE2_ARCHITECTURE.md)
 * -- consolidates what used to be the standalone History screen (now the
 * Documents tab) with Products, Customers, and a Business Profile summary,
 * all backed by [WorkspaceViewModel] reading the Sprint 1 repositories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(navController: NavHostController) {
    val container = LocalAppContainer.current
    val viewModel: WorkspaceViewModel = viewModel(
        factory = SimpleViewModelFactory {
            WorkspaceViewModel(
                container.workspaceRepository,
                container.documentRepository,
                container.productServiceRepository,
                container.customerRepository,
                container.businessProfileRepository,
                container.brandSettingsRepository
            )
        }
    )
    var selectedTab by remember { mutableStateOf(0) }

    var editingProduct by remember { mutableStateOf<ProductService?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var showCustomerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Workspace") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    TAB_TITLES.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                1 -> FloatingActionButton(onClick = { editingProduct = null; showProductDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add product/service")
                }
                2 -> FloatingActionButton(onClick = { editingCustomer = null; showCustomerDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add customer")
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "workspace_tab"
        ) { tab ->
            when (tab) {
                0 -> DocumentsTab(viewModel, navController)
                1 -> ProductsTab(viewModel) { editingProduct = it; showProductDialog = true }
                2 -> CustomersTab(viewModel) { editingCustomer = it; showCustomerDialog = true }
                else -> BusinessProfileTab(viewModel, navController)
            }
        }
    }

    if (showProductDialog) {
        ProductDialog(
            product = editingProduct,
            workspaceId = "", // stamped onto the real workspace by WorkspaceViewModel.saveProduct
            onDismiss = { showProductDialog = false },
            onSave = {
                viewModel.saveProduct(it)
                showProductDialog = false
            }
        )
    }
    if (showCustomerDialog) {
        CustomerDialog(
            customer = editingCustomer,
            workspaceId = "", // stamped onto the real workspace by WorkspaceViewModel.saveCustomer
            onDismiss = { showCustomerDialog = false },
            onSave = {
                viewModel.saveCustomer(it)
                showCustomerDialog = false
            }
        )
    }
}

@Composable
private fun DocumentsTab(viewModel: WorkspaceViewModel, navController: NavHostController) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            label = { Text("Search documents") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        if (documents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (query.isBlank()) {
                        "No documents yet. Generate something from the home screen!"
                    } else {
                        "No documents match \"$query\"."
                    },
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(documents, key = { it.id }) { doc ->
                    DocumentRow(doc, onClick = { navController.navigate(Routes.result(doc.id)) }, onDelete = { viewModel.deleteDocument(doc.id) })
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(doc: GeneratedDocument, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(text = doc.toolTitle, style = MaterialTheme.typography.labelMedium)
                Text(text = doc.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(doc.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun ProductsTab(viewModel: WorkspaceViewModel, onEdit: (ProductService) -> Unit) {
    val products by viewModel.products.collectAsStateWithLifecycle()

    if (products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No products or services yet. Tap + to add one.", modifier = Modifier.padding(32.dp))
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(products, key = { it.id }) { product ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { onEdit(product) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = true)) {
                        Text(text = product.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = listOfNotNull(
                                if (product.type == ProductServiceType.PRODUCT) "Product" else "Service",
                                product.price.takeIf { it != 0.0 }?.let { "₦$it" },
                                product.unit.ifBlank { null }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.deleteProduct(product.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${product.name}")
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomersTab(viewModel: WorkspaceViewModel, onEdit: (Customer) -> Unit) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    if (customers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No customers yet. Tap + to add one.", modifier = Modifier.padding(32.dp))
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(customers, key = { it.id }) { customer ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { onEdit(customer) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = true)) {
                        Text(text = customer.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = listOfNotNull(customer.companyName, customer.phone, customer.email)
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.deleteCustomer(customer.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${customer.name}")
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessProfileTab(viewModel: WorkspaceViewModel, navController: NavHostController) {
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val brand by viewModel.brandSettings.collectAsStateWithLifecycle()

    val nonNullProfile = profile
    if (nonNullProfile == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("You haven't set up your business profile yet.", style = MaterialTheme.typography.titleMedium)
            Text(
                "Set it up once and every AI tool can use it -- no more retyping your business details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            Button(onClick = { navController.navigate(Routes.BUSINESS_SETUP) }) {
                Text("Get started")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = nonNullProfile.businessName, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = listOfNotNull(nonNullProfile.businessType.ifBlank { null }, nonNullProfile.industry.ifBlank { null })
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (nonNullProfile.description.isNotBlank()) {
            ProfileField("Customers", nonNullProfile.description)
        }
        val location = listOfNotNull(
            nonNullProfile.city.ifBlank { null },
            nonNullProfile.state.ifBlank { null },
            nonNullProfile.country.ifBlank { null }
        ).joinToString(", ")
        if (location.isNotBlank()) ProfileField("Location", location)
        brand?.let {
            ProfileField("Brand tone", it.tone.name.lowercase().replaceFirstChar(Char::uppercase))
            if (it.tagline.isNotBlank()) ProfileField("Tagline", it.tagline)
        }
        nonNullProfile.phone?.let { ProfileField("Phone", it) }
        nonNullProfile.email?.let { ProfileField("Email", it) }
        nonNullProfile.whatsapp?.let { ProfileField("WhatsApp", it) }
        nonNullProfile.website?.let { ProfileField("Website", it) }
        nonNullProfile.address?.let { ProfileField("Address", it) }

        OutlinedButton(
            onClick = { navController.navigate(Routes.BUSINESS_SETUP) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Edit")
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

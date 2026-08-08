package com.ai4biz.app.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ai4biz.app.model.Customer
import com.ai4biz.app.model.ProductService
import com.ai4biz.app.model.ProductServiceType
import java.util.UUID

private const val WIZARD_CURRENCY = "NGN"

/** Add/edit form for a [ProductService] -- [product] null means "add new". */
@Composable
fun ProductDialog(
    product: ProductService?,
    workspaceId: String,
    onDismiss: () -> Unit,
    onSave: (ProductService) -> Unit
) {
    var name by remember { mutableStateOf(product?.name.orEmpty()) }
    var type by remember { mutableStateOf(product?.type ?: ProductServiceType.PRODUCT) }
    var price by remember { mutableStateOf(product?.price?.takeIf { it != 0.0 }?.toString().orEmpty()) }
    var unit by remember { mutableStateOf(product?.unit.orEmpty()) }
    var description by remember { mutableStateOf(product?.description.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Add product/service" else "Edit product/service") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == ProductServiceType.PRODUCT,
                        onClick = { type = ProductServiceType.PRODUCT },
                        label = { Text("Product") }
                    )
                    FilterChip(
                        selected = type == ProductServiceType.SERVICE,
                        onClick = { type = ProductServiceType.SERVICE },
                        label = { Text("Service") }
                    )
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (optional)") },
                    placeholder = { Text("e.g. per box") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val now = System.currentTimeMillis()
                    onSave(
                        ProductService(
                            id = product?.id ?: UUID.randomUUID().toString(),
                            workspaceId = workspaceId,
                            name = name.trim(),
                            type = type,
                            description = description.trim(),
                            category = "",
                            price = price.toDoubleOrNull() ?: 0.0,
                            currency = WIZARD_CURRENCY,
                            unit = unit.trim(),
                            isActive = true,
                            createdAt = product?.createdAt ?: now,
                            updatedAt = now
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Add/edit form for a [Customer] -- [customer] null means "add new". */
@Composable
fun CustomerDialog(
    customer: Customer?,
    workspaceId: String,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name.orEmpty()) }
    var companyName by remember { mutableStateOf(customer?.companyName.orEmpty()) }
    var email by remember { mutableStateOf(customer?.email.orEmpty()) }
    var phone by remember { mutableStateOf(customer?.phone.orEmpty()) }
    var whatsapp by remember { mutableStateOf(customer?.whatsapp.orEmpty()) }
    var industry by remember { mutableStateOf(customer?.industry.orEmpty()) }
    var notes by remember { mutableStateOf(customer?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customer == null) "Add customer" else "Edit customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = industry,
                    onValueChange = { industry = it },
                    label = { Text("Industry (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val now = System.currentTimeMillis()
                    onSave(
                        Customer(
                            id = customer?.id ?: UUID.randomUUID().toString(),
                            workspaceId = workspaceId,
                            name = name.trim(),
                            companyName = companyName.trim().ifBlank { null },
                            email = email.trim().ifBlank { null },
                            phone = phone.trim().ifBlank { null },
                            whatsapp = whatsapp.trim().ifBlank { null },
                            industry = industry.trim().ifBlank { null },
                            notes = notes.trim().ifBlank { null },
                            createdAt = customer?.createdAt ?: now,
                            updatedAt = now
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

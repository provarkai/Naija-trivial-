package com.ai4biz.app.ui.businesssetup.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ai4biz.app.model.BrandTone
import com.ai4biz.app.model.BusinessGoalType
import com.ai4biz.app.model.ProductServiceType
import com.ai4biz.app.ui.businesssetup.BusinessSetupViewModel
import com.ai4biz.app.ui.businesssetup.ProductDraft

private val BUSINESS_TYPE_OPTIONS = listOf(
    "Sole proprietor", "Limited company", "Partnership", "Freelancer", "Startup", "Other"
)

private val INDUSTRY_OPTIONS = listOf(
    "Agriculture", "Retail", "Real Estate", "Construction", "Technology",
    "Professional Services", "Food & Hospitality", "Fashion & Beauty",
    "Education", "Health", "Logistics", "Manufacturing", "Finance", "Other"
)

private fun BrandTone.label(): String = when (this) {
    BrandTone.PROFESSIONAL -> "Professional"
    BrandTone.FRIENDLY -> "Friendly"
    BrandTone.PREMIUM -> "Premium"
    BrandTone.CASUAL -> "Casual"
    BrandTone.CORPORATE -> "Corporate"
    BrandTone.BOLD -> "Bold"
    BrandTone.PERSUASIVE -> "Persuasive"
    BrandTone.CUSTOM -> "Custom"
}

private fun BusinessGoalType.label(): String = when (this) {
    BusinessGoalType.GET_MORE_CUSTOMERS -> "Get more customers"
    BusinessGoalType.INCREASE_SALES -> "Increase sales"
    BusinessGoalType.IMPROVE_MARKETING -> "Improve marketing"
    BusinessGoalType.SAVE_TIME -> "Save time"
    BusinessGoalType.CUSTOMER_RETENTION -> "Improve customer service"
    BusinessGoalType.BRAND_AWARENESS -> "Build my brand"
    BusinessGoalType.AUTOMATION -> "Automate repetitive work"
    BusinessGoalType.EXPANSION -> "Expand my business"
}

/** A row of single-select chips -- tapping a chip selects it, replacing any prior selection. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleSelectChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(option) })
        }
    }
}

/** A row of multi-select chips -- tapping toggles membership via [onToggle]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> MultiSelectChipRow(options: List<T>, selected: List<T>, label: (T) -> String, onToggle: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option in selected, onClick = { onToggle(option) }, label = { Text(label(option)) })
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Text(text = title, style = MaterialTheme.typography.headlineSmall)
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
    )
}

@Composable
fun BusinessBasicsStep(viewModel: BusinessSetupViewModel) {
    Column {
        StepHeader("What do you do?", "Tell us your business name and how it's structured.")
        OutlinedTextField(
            value = viewModel.businessName,
            onValueChange = { viewModel.businessName = it },
            label = { Text("Business name") },
            placeholder = { Text("e.g. GreenLeaf Farms") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Business type", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        SingleSelectChipRow(BUSINESS_TYPE_OPTIONS, viewModel.businessType) { viewModel.businessType = it }
    }
}

@Composable
fun IndustryStep(viewModel: BusinessSetupViewModel) {
    Column {
        StepHeader("What industry are you in?", "This helps us tailor AI output to your business.")
        SingleSelectChipRow(INDUSTRY_OPTIONS, viewModel.industry) { viewModel.industry = it }
    }
}

@Composable
fun ProductsStep(viewModel: BusinessSetupViewModel) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ProductServiceType.PRODUCT) }
    var price by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    Column {
        StepHeader("What do you sell?", "Add the products or services you offer. You can skip this and add them later.")

        viewModel.products.forEach { draft ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = draft.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = listOfNotNull(
                                if (draft.type == ProductServiceType.PRODUCT) "Product" else "Service",
                                draft.price.ifBlank { null }?.let { "₦$it" },
                                draft.unit.ifBlank { null }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.removeProduct(draft) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ${draft.name}")
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            placeholder = { Text("e.g. Organic vegetable box") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        SingleSelectChipRow(
            listOf("Product", "Service"),
            if (type == ProductServiceType.PRODUCT) "Product" else "Service"
        ) { type = if (it == "Product") ProductServiceType.PRODUCT else ProductServiceType.SERVICE }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Price (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it },
                label = { Text("Unit (optional)") },
                placeholder = { Text("e.g. per box") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            enabled = name.isNotBlank(),
            onClick = {
                viewModel.addProduct(ProductDraft(name = name.trim(), type = type, price = price.trim(), unit = unit.trim()))
                name = ""
                price = ""
                unit = ""
                type = ProductServiceType.PRODUCT
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add")
        }
    }
}

@Composable
fun TargetCustomersStep(viewModel: BusinessSetupViewModel) {
    Column {
        StepHeader("Who are your customers?", "Describe who you serve -- this helps AI-generated content speak to the right audience.")
        OutlinedTextField(
            value = viewModel.targetCustomers,
            onValueChange = { viewModel.targetCustomers = it },
            label = { Text("Target customers (optional)") },
            placeholder = { Text("e.g. Restaurants, hotels, busy families and health-conscious consumers") },
            singleLine = false,
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LocationStep(viewModel: BusinessSetupViewModel) {
    Column {
        StepHeader("Where's your business based?", "This helps localize currency, terminology and recommendations.")
        OutlinedTextField(
            value = viewModel.country,
            onValueChange = { viewModel.country = it },
            label = { Text("Country") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.state,
            onValueChange = { viewModel.state = it },
            label = { Text("State") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.city,
            onValueChange = { viewModel.city = it },
            label = { Text("City") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BrandPersonalityStep(viewModel: BusinessSetupViewModel) {
    Column {
        StepHeader("What's your brand personality?", "AI-generated content will match this tone.")
        SingleSelectChipRow(BrandTone.entries.map { it.label() }, viewModel.tone.label()) { label ->
            viewModel.tone = BrandTone.entries.first { it.label() == label }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.tagline,
            onValueChange = { viewModel.tagline = it },
            label = { Text("Tagline (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (viewModel.tone == BrandTone.CUSTOM) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.customWritingStyle,
                onValueChange = { viewModel.customWritingStyle = it },
                label = { Text("Describe your tone") },
                placeholder = { Text("e.g. Warm, witty, a little irreverent") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BusinessGoalsStep(viewModel: BusinessSetupViewModel) {
    Column {
        StepHeader("What are your business goals?", "Pick up to 3 -- optional, but helps AI prioritize suggestions.")
        MultiSelectChipRow(BusinessGoalType.entries, viewModel.selectedGoals, { it.label() }) { viewModel.toggleGoal(it) }
    }
}

@Composable
fun ContactInfoStep(viewModel: BusinessSetupViewModel) {
    Column {
        StepHeader("How can customers reach you?", "All optional -- this can automatically populate proposals and invoices.")
        OutlinedTextField(
            value = viewModel.phone,
            onValueChange = { viewModel.phone = it },
            label = { Text("Phone (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Email (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.whatsapp,
            onValueChange = { viewModel.whatsapp = it },
            label = { Text("WhatsApp (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.website,
            onValueChange = { viewModel.website = it },
            label = { Text("Website (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.address,
            onValueChange = { viewModel.address = it },
            label = { Text("Address (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

package com.ai4biz.app.ui.businesssetup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai4biz.app.data.repository.BrandSettingsRepository
import com.ai4biz.app.data.repository.BusinessGoalRepository
import com.ai4biz.app.data.repository.BusinessProfileRepository
import com.ai4biz.app.data.repository.OnboardingRepository
import com.ai4biz.app.data.repository.ProductServiceRepository
import com.ai4biz.app.data.repository.WorkspaceRepository
import com.ai4biz.app.model.BrandSettings
import com.ai4biz.app.model.BrandTone
import com.ai4biz.app.model.BusinessGoal
import com.ai4biz.app.model.BusinessGoalType
import com.ai4biz.app.model.BusinessProfile
import com.ai4biz.app.model.ProductService
import com.ai4biz.app.model.ProductServiceType
import com.ai4biz.app.model.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

private const val MAX_SELECTED_GOALS = 3
private const val WIZARD_CURRENCY = "NGN"

/** A product/service the wizard is drafting -- not persisted until [BusinessSetupViewModel.finish]. */
data class ProductDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val type: ProductServiceType = ProductServiceType.PRODUCT,
    val price: String = "",
    val unit: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Owns all 8 steps of the Business Setup wizard (Phase 2 Sprint 2, see
 * docs/PHASE2_ARCHITECTURE.md). One instance for the whole wizard route --
 * nothing is written to any repository until [finish] or [skip], so an
 * abandoned wizard never leaves a half-valid [BusinessProfile] behind.
 */
class BusinessSetupViewModel(
    private val workspaceRepository: WorkspaceRepository,
    private val businessProfileRepository: BusinessProfileRepository,
    private val brandSettingsRepository: BrandSettingsRepository,
    private val productServiceRepository: ProductServiceRepository,
    private val businessGoalRepository: BusinessGoalRepository,
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    var currentStep by mutableStateOf(0)
        private set

    // Step 1 -- Business Basics
    var businessName by mutableStateOf("")
    var businessType by mutableStateOf("")

    // Step 2 -- Industry
    var industry by mutableStateOf("")

    // Step 3 -- Products/Services
    val products = mutableStateListOf<ProductDraft>()

    // Step 4 -- Target Customers
    var targetCustomers by mutableStateOf("")

    // Step 5 -- Location
    var country by mutableStateOf("Nigeria")
    var state by mutableStateOf("")
    var city by mutableStateOf("")

    // Step 6 -- Brand Personality
    var tone by mutableStateOf(BrandTone.PROFESSIONAL)
    var tagline by mutableStateOf("")
    var customWritingStyle by mutableStateOf("")

    // Step 7 -- Business Goals (order = priority)
    val selectedGoals = mutableStateListOf<BusinessGoalType>()

    // Step 8 -- Contact Info
    var phone by mutableStateOf("")
    var email by mutableStateOf("")
    var website by mutableStateOf("")
    var whatsapp by mutableStateOf("")
    var address by mutableStateOf("")

    private val _isLoadingExisting = MutableStateFlow(true)
    val isLoadingExisting: StateFlow<Boolean> = _isLoadingExisting.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // Populated by loadExisting() when re-entering to edit -- preserved
    // verbatim on save since this wizard has no field for them.
    private var currentWorkspace: Workspace? = null
    private var existingProfileId: String? = null
    private var existingProfileCreatedAt: Long? = null
    private var existingTaxNumber: String? = null
    private var existingBrandSettingsId: String? = null
    private var existingPrimaryColor: String? = null
    private var existingSecondaryColor: String? = null
    private var existingDefaultLanguage: String? = null
    private var existingLogoUri: String? = null
    private var existingProductIds: Set<String> = emptySet()
    private val removedExistingProductIds = mutableSetOf<String>()
    private var existingGoalIds: List<String> = emptyList()

    val canProceedFromCurrentStep: Boolean
        get() = when (currentStep) {
            0 -> businessName.isNotBlank() && businessType.isNotBlank()
            1 -> industry.isNotBlank()
            5 -> tone != BrandTone.CUSTOM || customWritingStyle.isNotBlank()
            else -> true
        }

    init {
        viewModelScope.launch { loadExisting() }
    }

    private suspend fun loadExisting() {
        val workspace = workspaceRepository.observeActiveWorkspace().first()
            ?: workspaceRepository.getOrCreateDefaultWorkspace()
        currentWorkspace = workspace

        businessProfileRepository.observeAllByWorkspace(workspace.id).first().firstOrNull()?.let { profile ->
            existingProfileId = profile.id
            existingProfileCreatedAt = profile.createdAt
            existingTaxNumber = profile.taxNumber
            businessName = profile.businessName
            businessType = profile.businessType
            industry = profile.industry
            targetCustomers = profile.description
            country = profile.country
            state = profile.state
            city = profile.city
            address = profile.address.orEmpty()
            phone = profile.phone.orEmpty()
            email = profile.email.orEmpty()
            website = profile.website.orEmpty()
            whatsapp = profile.whatsapp.orEmpty()
        }

        brandSettingsRepository.observeAllByWorkspace(workspace.id).first().firstOrNull()?.let { brand ->
            existingBrandSettingsId = brand.id
            existingPrimaryColor = brand.primaryColor
            existingSecondaryColor = brand.secondaryColor
            existingDefaultLanguage = brand.defaultLanguage
            existingLogoUri = brand.logoUri
            tone = brand.tone
            tagline = brand.tagline
            if (brand.tone == BrandTone.CUSTOM) customWritingStyle = brand.writingStyle
        }

        val existingProducts = productServiceRepository.observeAllByWorkspace(workspace.id).first()
        existingProductIds = existingProducts.map { it.id }.toSet()
        products.clear()
        products.addAll(
            existingProducts.map {
                ProductDraft(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    price = if (it.price == 0.0) "" else it.price.toString(),
                    unit = it.unit,
                    description = it.description,
                    createdAt = it.createdAt
                )
            }
        )

        val existingGoals = businessGoalRepository.observeAllByWorkspace(workspace.id).first().sortedBy { it.priority }
        existingGoalIds = existingGoals.map { it.id }
        selectedGoals.clear()
        selectedGoals.addAll(existingGoals.map { it.goalType })

        _isLoadingExisting.value = false
    }

    fun nextStep() {
        if (currentStep < 7) currentStep++
    }

    fun previousStep() {
        if (currentStep > 0) currentStep--
    }

    fun addProduct(draft: ProductDraft) {
        products.add(draft)
    }

    fun removeProduct(draft: ProductDraft) {
        products.remove(draft)
        if (draft.id in existingProductIds) removedExistingProductIds.add(draft.id)
    }

    /** Caps selection at [MAX_SELECTED_GOALS]; toggling a selected goal off always works. */
    fun toggleGoal(type: BusinessGoalType) {
        if (type in selectedGoals) {
            selectedGoals.remove(type)
        } else if (selectedGoals.size < MAX_SELECTED_GOALS) {
            selectedGoals.add(type)
        }
    }

    suspend fun finish() {
        _isSaving.value = true
        try {
            val workspace = currentWorkspace ?: workspaceRepository.getOrCreateDefaultWorkspace()
            val now = System.currentTimeMillis()

            val profile = BusinessProfile(
                id = existingProfileId ?: UUID.randomUUID().toString(),
                workspaceId = workspace.id,
                businessName = businessName.trim(),
                businessType = businessType,
                industry = industry,
                description = targetCustomers.trim(),
                country = country.trim(),
                state = state.trim(),
                city = city.trim(),
                address = address.trim().ifBlank { null },
                phone = phone.trim().ifBlank { null },
                email = email.trim().ifBlank { null },
                website = website.trim().ifBlank { null },
                whatsapp = whatsapp.trim().ifBlank { null },
                currency = WIZARD_CURRENCY,
                taxNumber = existingTaxNumber,
                createdAt = existingProfileCreatedAt ?: now,
                updatedAt = now
            )
            businessProfileRepository.save(profile)

            val brand = BrandSettings(
                id = existingBrandSettingsId ?: UUID.randomUUID().toString(),
                workspaceId = workspace.id,
                brandName = businessName.trim(),
                tagline = tagline.trim(),
                tone = tone,
                writingStyle = if (tone == BrandTone.CUSTOM) customWritingStyle.trim() else tone.name,
                primaryColor = existingPrimaryColor ?: "#4F46E5",
                secondaryColor = existingSecondaryColor ?: "#0EA5E9",
                defaultLanguage = existingDefaultLanguage ?: "en",
                logoUri = existingLogoUri
            )
            brandSettingsRepository.save(brand)

            products.forEach { draft ->
                productServiceRepository.save(
                    ProductService(
                        id = draft.id,
                        workspaceId = workspace.id,
                        name = draft.name.trim(),
                        type = draft.type,
                        description = draft.description.trim(),
                        category = "",
                        price = draft.price.toDoubleOrNull() ?: 0.0,
                        currency = WIZARD_CURRENCY,
                        unit = draft.unit.trim(),
                        isActive = true,
                        createdAt = draft.createdAt,
                        updatedAt = now
                    )
                )
            }
            removedExistingProductIds.forEach { productServiceRepository.delete(it) }

            // Goals have no identity beyond type+priority, so delete-then-recreate
            // is simpler and fully correct for a max-3-item set.
            existingGoalIds.forEach { businessGoalRepository.delete(it) }
            selectedGoals.forEachIndexed { index, goalType ->
                businessGoalRepository.save(
                    BusinessGoal(
                        id = UUID.randomUUID().toString(),
                        workspaceId = workspace.id,
                        goalType = goalType,
                        description = "",
                        priority = index + 1,
                        isActive = true
                    )
                )
            }

            workspaceRepository.save(workspace.copy(businessProfileId = profile.id, updatedAt = now))
            onboardingRepository.setHasCompletedBusinessSetup(true)
        } finally {
            _isSaving.value = false
        }
    }

    suspend fun skip() {
        onboardingRepository.setHasCompletedBusinessSetup(true)
    }
}

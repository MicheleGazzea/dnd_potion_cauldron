package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

class PotionViewModel(private val repository: PotionRepository) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }
    }

    val settings: StateFlow<CampaignSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allRecipes: StateFlow<List<PotionRecipe>> = repository.allRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeBrews: StateFlow<List<ActiveBrew>> = repository.activeBrews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedBrews: StateFlow<List<ActiveBrew>> = repository.archivedBrews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableResidues: StateFlow<List<ResidueItem>> = repository.availableResidues
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allResidues: StateFlow<List<ResidueItem>> = repository.allResidues
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun advanceTimeBlock() {
        val s = settings.value ?: return
        viewModelScope.launch {
            val currentAbs = CampaignCalendar.getAbsolutePeriods(
                s.currentYear,
                s.currentMonth,
                s.currentDay,
                s.currentPeriod
            )
            val nextState = CampaignCalendar.getCalendarFromAbsolutePeriods(currentAbs + 1)
            repository.updateCampaignTime(
                nextState.year,
                nextState.month,
                nextState.day,
                nextState.period
            )
        }
    }

    fun decrementTimeBlock() {
        val s = settings.value ?: return
        viewModelScope.launch {
            val currentAbs = CampaignCalendar.getAbsolutePeriods(
                s.currentYear,
                s.currentMonth,
                s.currentDay,
                s.currentPeriod
            )
            if (currentAbs > 0) {
                val prevState = CampaignCalendar.getCalendarFromAbsolutePeriods(currentAbs - 1)
                repository.updateCampaignTime(
                    prevState.year,
                    prevState.month,
                    prevState.day,
                    prevState.period
                )
            }
        }
    }

    fun startBrewing(
        recipe: PotionRecipe,
        quantity: Int,
        selectedResidue: ResidueItem?,
        startYear: Int,
        startMonth: Int,
        startDay: Int,
        startPeriod: String
    ) {
        val baseDuration = calculateBaseDuration(recipe.craftingPriceGp)
        
        viewModelScope.launch {
            var actualDuration = baseDuration
            var d100Roll: Int? = null
            var d100Effect: String? = null
            var potionName = recipe.name

            if (selectedResidue != null) {
                // Mix in residue: 1d100
                val roll = Random.nextInt(1, 101)
                d100Roll = roll
                when (roll) {
                    in 1..33 -> {
                        d100Effect = "Accelerated Batch. Cooking duration is halved!"
                        actualDuration = baseDuration / 2.0
                    }
                    in 34..66 -> {
                        d100Effect = "Potency Strain. Output is 50% more potent!"
                    }
                    in 67..99 -> {
                        // Harvest a free random potion of the same rarity
                        val sameRarityRecipes = allRecipes.value.filter { it.rarity == recipe.rarity }
                        val bonusPotion = if (sameRarityRecipes.isNotEmpty()) {
                            sameRarityRecipes.random().name
                        } else {
                            recipe.name
                        }
                        d100Effect = "Secondary Separation. Bonus potion gained: $bonusPotion!"
                    }
                    100 -> {
                        // Mutates into a random "Rare" potion
                        val rareRecipes = allRecipes.value.filter { it.rarity == "Rare" }
                        val mutatedPotion = if (rareRecipes.isNotEmpty()) {
                            rareRecipes.random().name
                        } else {
                            "Potion of Mind Reading"
                        }
                        potionName = mutatedPotion
                        d100Effect = "Supermutation! Yeast mutated into item: $mutatedPotion!"
                    }
                }

                // Mark residue as used
                repository.updateResidue(selectedResidue.copy(isUsed = true))
            }

            val brew = ActiveBrew(
                potionName = potionName,
                startYear = startYear,
                startMonth = startMonth,
                startDay = startDay,
                startPeriod = startPeriod,
                durationDays = baseDuration,
                batchQuantity = quantity,
                appliedResidueId = selectedResidue?.id,
                appliedResidueName = selectedResidue?.potionSource,
                appliedResidueType = selectedResidue?.residueType,
                d100Roll = d100Roll,
                d100Effect = d100Effect,
                actualDurationDays = actualDuration,
                isCompleted = false
            )
            repository.addActiveBrew(brew)
        }
    }

    fun resolveBrew(brew: ActiveBrew, d20RollInput: Int) {
        viewModelScope.launch {
            val quantity = brew.batchQuantity
            var finalQuantity = quantity
            var outcomeText = ""
            var rewardResidue: ResidueItem? = null

            when (d20RollInput) {
                1 -> {
                    finalQuantity = floor(quantity * 0.5).toInt()
                    outcomeText = "NAT 1 ruined half the batch. Yield: $finalQuantity potions."
                }
                in 2..5 -> {
                    finalQuantity = quantity
                    outcomeText = "Low Quality. 25% chance of diluted potency, normal yield: $finalQuantity."
                }
                in 6..15 -> {
                    finalQuantity = quantity
                    outcomeText = "Standard Completion. Brewed $finalQuantity potions successfully."
                }
                in 16..19 -> {
                    finalQuantity = quantity
                    outcomeText = "Great Success! Brewed $finalQuantity potions + Standard Residue."
                    rewardResidue = ResidueItem(
                        potionSource = brew.potionName,
                        residueType = "Standard",
                        isUsed = false
                    )
                }
                20 -> {
                    finalQuantity = ceil(quantity * 1.5).toInt()
                    outcomeText = "NAT 20 Masterwork Dilution! Yield: $finalQuantity potions + Catalyst."
                    rewardResidue = ResidueItem(
                        potionSource = brew.potionName,
                        residueType = "Masterwork Catalyst",
                        isUsed = false
                    )
                }
            }

            // Insert standard or masterwork catalyst reward if generated
            rewardResidue?.let {
                repository.addResidue(it)
            }

            // Combine mutations, separations or effects inside final notes log
            var combinedNotes = outcomeText
            brew.d100Effect?.let { effect ->
                combinedNotes = "[$effect] - $combinedNotes"
            }

            // Update active brew to completed state
            val completedBrew = brew.copy(
                d20Roll = d20RollInput,
                isCompleted = true,
                finalQuantity = finalQuantity,
                notes = combinedNotes
            )

            // Save back
            repository.updateActiveBrew(completedBrew)
        }
    }

    fun cancelBrew(brew: ActiveBrew) {
        viewModelScope.launch {
            repository.deleteActiveBrew(brew)
        }
    }

    fun calculateBaseDuration(priceGp: Int): Double {
        return sqrt(priceGp.toDouble()) / 5.0
    }

    class Factory(private val repository: PotionRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PotionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PotionViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

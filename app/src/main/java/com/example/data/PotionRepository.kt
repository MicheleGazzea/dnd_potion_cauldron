package com.example.data

import kotlinx.coroutines.flow.Flow

class PotionRepository(private val db: PotionDatabase) {
    private val settingsDao = db.settingsDao()
    private val recipeDao = db.recipeDao()
    private val brewDao = db.brewDao()
    private val residueDao = db.residueDao()

    val settings: Flow<CampaignSettings?> = settingsDao.getSettings()
    val allRecipes: Flow<List<PotionRecipe>> = recipeDao.getAllRecipes()
    val activeBrews: Flow<List<ActiveBrew>> = brewDao.getActiveBrews()
    val archivedBrews: Flow<List<ActiveBrew>> = brewDao.getArchivedBrews()
    val availableResidues: Flow<List<ResidueItem>> = residueDao.getAvailableResidues()
    val allResidues: Flow<List<ResidueItem>> = residueDao.getAllResidues()

    suspend fun checkAndSeedDatabase() {
        val count = recipeDao.countRecipes()
        if (count == 0) {
            recipeDao.insertRecipes(PrepopulatedData.recipes)
        }
        val currentSettings = settingsDao.getSettingsDirect()
        if (currentSettings == null) {
            settingsDao.insertOrUpdate(
                CampaignSettings(
                    id = 1,
                    currentYear = 2023,
                    currentMonth = 9,
                    currentDay = 17,
                    currentPeriod = "NOON"
                )
            )
        }
    }

    suspend fun updateCampaignTime(year: Int, month: Int, day: Int, period: String) {
        settingsDao.insertOrUpdate(
            CampaignSettings(
                id = 1,
                currentYear = year,
                currentMonth = month,
                currentDay = day,
                currentPeriod = period
            )
        )
    }

    suspend fun addActiveBrew(brew: ActiveBrew) {
        brewDao.insertBrew(brew)
    }

    suspend fun updateActiveBrew(brew: ActiveBrew) {
        brewDao.updateBrew(brew)
    }

    suspend fun deleteActiveBrew(brew: ActiveBrew) {
        brewDao.deleteBrew(brew)
    }

    suspend fun addResidue(residue: ResidueItem) {
        residueDao.insertResidue(residue)
    }

    suspend fun updateResidue(residue: ResidueItem) {
        residueDao.updateResidue(residue)
    }
}

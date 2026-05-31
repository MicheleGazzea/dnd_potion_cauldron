package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignSettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<CampaignSettings?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): CampaignSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: CampaignSettings)
}

@Dao
interface PotionRecipeDao {
    @Query("SELECT * FROM potions_library ORDER BY name ASC")
    fun getAllRecipes(): Flow<List<PotionRecipe>>

    @Query("SELECT * FROM potions_library ORDER BY name ASC")
    suspend fun getAllRecipesDirect(): List<PotionRecipe>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<PotionRecipe>)

    @Query("SELECT COUNT(*) FROM potions_library")
    suspend fun countRecipes(): Int
}

@Dao
interface ActiveBrewDao {
    @Query("SELECT * FROM active_brews WHERE is_completed = 0 ORDER BY timestamp DESC")
    fun getActiveBrews(): Flow<List<ActiveBrew>>

    @Query("SELECT * FROM active_brews WHERE is_completed = 1 ORDER BY timestamp DESC")
    fun getArchivedBrews(): Flow<List<ActiveBrew>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrew(brew: ActiveBrew): Long

    @Update
    suspend fun updateBrew(brew: ActiveBrew)

    @Query("SELECT * FROM active_brews WHERE id = :id")
    suspend fun getBrewById(id: Int): ActiveBrew?

    @Delete
    suspend fun deleteBrew(brew: ActiveBrew)
}

@Dao
interface ResidueInventoryDao {
    @Query("SELECT * FROM residue_inventory WHERE is_used = 0 ORDER BY timestamp DESC")
    fun getAvailableResidues(): Flow<List<ResidueItem>>

    @Query("SELECT * FROM residue_inventory ORDER BY timestamp DESC")
    fun getAllResidues(): Flow<List<ResidueItem>>

    @Query("SELECT * FROM residue_inventory WHERE id = :id")
    suspend fun getResidueById(id: Int): ResidueItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResidue(residue: ResidueItem): Long

    @Update
    suspend fun updateResidue(residue: ResidueItem)
}

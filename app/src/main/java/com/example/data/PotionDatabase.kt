package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CampaignSettings::class,
        PotionRecipe::class,
        ActiveBrew::class,
        ResidueItem::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PotionDatabase : RoomDatabase() {
    abstract fun settingsDao(): CampaignSettingsDao
    abstract fun recipeDao(): PotionRecipeDao
    abstract fun brewDao(): ActiveBrewDao
    abstract fun residueDao(): ResidueInventoryDao

    companion object {
        @Volatile
        private var INSTANCE: PotionDatabase? = null

        fun getDatabase(context: Context): PotionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PotionDatabase::class.java,
                    "potion_brewing_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

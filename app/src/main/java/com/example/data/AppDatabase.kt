package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * AppDatabase provides the primary Room access point to the underlying SQLite database
 * for the Smart Grocery & Expense Tracker app.
 */
@Database(
    entities = [
        ProfileEntity::class,
        ExpenseEntity::class,
        ShoppingListItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Abstract methods to expose DAOs to the application
    abstract fun profileDao(): ProfileDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun shoppingListDao(): ShoppingListDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `profiles` ADD COLUMN `monthlyBudget` REAL NOT NULL DEFAULT 0.0")
            }
        }

        /**
         * Returns the thread-safe singleton instance of [AppDatabase].
         * Uses fallbackToDestructiveMigration to clear database schema changes automatically if no direct migration is matching.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_grocery_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

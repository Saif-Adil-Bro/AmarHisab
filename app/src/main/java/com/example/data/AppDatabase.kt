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
        ShoppingListItemEntity::class,
        CategoryEntity::class,
        RecurringExpenseEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Abstract methods to expose DAOs to the application
    abstract fun profileDao(): ProfileDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `profiles` ADD COLUMN `monthlyBudget` REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create categories table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `iconEmoji` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Pre-populate default categories
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('বাজার', '🛒', '#1565C0', 1)")
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('শাকসবজি', '🥬', '#2E7D32', 1)")
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('মাছ ও মাংস', '🍗', '#C62828', 1)")
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('যাতায়াত', '🚗', '#ED6C02', 1)")
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('বাসা ভাড়া', '🏠', '#4E342E', 1)")
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('বিদ্যুৎ বিল', '💡', '#EF6C00', 1)")
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('অন্যান্য', '💰', '#6A1B9A', 1)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `profileId` INTEGER NOT NULL,
                        `itemName` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL DEFAULT 'Grocery',
                        `frequency` TEXT NOT NULL,
                        `nextDueDate` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_expenses_profileId` ON `recurring_expenses` (`profileId`)")
            }
        }

        private val DATABASE_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate default categories for a new database install
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('বাজার', '🛒', '#1565C0', 1)")
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('শাকসবজি', '🥬', '#2E7D32', 1)")
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('মাছ ও মাংস', '🍗', '#C62828', 1)")
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('যাতায়াত', '🚗', '#ED6C02', 1)")
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('বাসা ভাড়া', '🏠', '#4E342E', 1)")
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('বিদ্যুৎ বিল', '💡', '#EF6C00', 1)")
                db.execSQL("INSERT INTO `categories` (`name`, `iconEmoji`, `colorHex`, `isDefault`) VALUES ('অন্যান্য', '💰', '#6A1B9A', 1)")
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(DATABASE_CALLBACK)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

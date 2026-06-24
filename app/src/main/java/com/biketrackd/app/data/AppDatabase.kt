package com.biketrackd.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PedalSession::class, Bike::class, MaintenancePart::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pedalSessionDao(): PedalSessionDao
    abstract fun bikeDao(): BikeDao
    abstract fun maintenancePartDao(): MaintenancePartDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `bike` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `model` TEXT NOT NULL DEFAULT '',
                        `type` TEXT NOT NULL DEFAULT '',
                        `acquisitionDate` INTEGER NOT NULL DEFAULT 0,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `isDefault` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("ALTER TABLE `pedal_history` ADD COLUMN `bikeId` INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `maintenance_part` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bikeId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `componentType` TEXT NOT NULL,
                        `lifespanKm` REAL NOT NULL,
                        `usedKm` REAL NOT NULL DEFAULT 0,
                        `installDate` INTEGER NOT NULL DEFAULT 0,
                        `notes` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY (`bikeId`) REFERENCES `bike`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_part_bikeId` ON `maintenance_part`(`bikeId`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gpsoss.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
            }
        }
    }
}

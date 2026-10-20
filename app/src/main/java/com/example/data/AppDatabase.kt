package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RepairRecord::class, RecordPhoto::class, CustomRepairType::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repairDao(): RepairDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Миграция с версии 1 на версию 2 без потери старых записей:
         * добавляет колонки status (по умолчанию 'Приёмка'), clientName, phone и statusChangedAt.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE repair_records ADD COLUMN status TEXT NOT NULL DEFAULT 'Приёмка'"
                )
                db.execSQL(
                    "ALTER TABLE repair_records ADD COLUMN clientName TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE repair_records ADD COLUMN phone TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE repair_records ADD COLUMN statusChangedAt TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /**
         * Миграция с версии 2 на версию 3 без потери старых записей и фотографий:
         * создаёт таблицу photos (id, recordId, filePath, position) со связью с repair_records
         * и переносит существующее фото каждой старой записи с position = 0.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordId` INTEGER NOT NULL, `filePath` TEXT NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`recordId`) REFERENCES `repair_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_photos_recordId` ON `photos` (`recordId`)"
                )
                db.execSQL(
                    "INSERT INTO `photos` (`recordId`, `filePath`, `position`) SELECT `id`, `photoPath`, 0 FROM `repair_records` WHERE `photoPath` IS NOT NULL AND TRIM(`photoPath`) != ''"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autoservice_records.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

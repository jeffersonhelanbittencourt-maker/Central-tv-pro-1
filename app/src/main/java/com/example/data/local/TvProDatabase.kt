package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.FavoriteEntity
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaylistSourceEntity
import com.example.data.model.WatchHistoryEntity

@Database(
    entities = [
        MediaItemEntity::class,
        PlaylistSourceEntity::class,
        FavoriteEntity::class,
        WatchHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TvProDatabase : RoomDatabase() {
    abstract fun tvProDao(): TvProDao

    companion object {
        @Volatile
        private var INSTANCE: TvProDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN isAvailable INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN year INTEGER")
                db.execSQL("ALTER TABLE media_items ADD COLUMN streamFormat TEXT NOT NULL DEFAULT 'HLS'")
                db.execSQL("ALTER TABLE media_items ADD COLUMN audioLanguage TEXT")
                db.execSQL("ALTER TABLE media_items ADD COLUMN hasSubtitles INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_items ADD COLUMN licenseStatus TEXT NOT NULL DEFAULT 'Domínio Público / Licença Aberta'")
                db.execSQL("ALTER TABLE media_items ADD COLUMN lastVerified INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): TvProDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TvProDatabase::class.java,
                    "central_tv_pro_ultimate.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

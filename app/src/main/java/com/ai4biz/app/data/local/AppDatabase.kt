package com.ai4biz.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        GeneratedDocumentEntity::class,
        WorkspaceEntity::class,
        BusinessProfileEntity::class,
        BrandSettingsEntity::class,
        ProductServiceEntity::class,
        BusinessGoalEntity::class,
        CustomerEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun generatedDocumentDao(): GeneratedDocumentDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun brandSettingsDao(): BrandSettingsDao
    abstract fun productServiceDao(): ProductServiceDao
    abstract fun businessGoalDao(): BusinessGoalDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Phase 2 Sprint 1 (docs/PHASE2_ARCHITECTURE.md): introduces the
         * Business Workspace data model. Creates the six new tables, seeds
         * one default workspace so a migrated install has somewhere for
         * existing documents to belong, then adds `workspaceId` to
         * `generated_documents` -- SQLite's `ALTER TABLE ... ADD COLUMN
         * ... DEFAULT` backfills every existing row with that default
         * value in the same statement, so no separate UPDATE is needed and
         * no existing document is lost or orphaned.
         *
         * No indices are added here deliberately -- Room validates any
         * `@Entity(indices = ...)` against what the migration actually
         * created, so introducing an index here without a matching
         * annotation (or vice versa) would crash on first launch after
         * upgrade. None of today's entities declare indices, so none are
         * created; add them together in a future migration once Sprint 2+
         * query patterns are known.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS workspaces (
                        id TEXT NOT NULL PRIMARY KEY,
                        ownerUserId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        businessProfileId TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS business_profiles (
                        id TEXT NOT NULL PRIMARY KEY,
                        workspaceId TEXT NOT NULL,
                        businessName TEXT NOT NULL,
                        businessType TEXT NOT NULL,
                        industry TEXT NOT NULL,
                        description TEXT NOT NULL,
                        country TEXT NOT NULL,
                        state TEXT NOT NULL,
                        city TEXT NOT NULL,
                        address TEXT,
                        phone TEXT,
                        email TEXT,
                        website TEXT,
                        whatsapp TEXT,
                        currency TEXT NOT NULL,
                        taxNumber TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS brand_settings (
                        id TEXT NOT NULL PRIMARY KEY,
                        workspaceId TEXT NOT NULL,
                        brandName TEXT NOT NULL,
                        tagline TEXT NOT NULL,
                        tone TEXT NOT NULL,
                        writingStyle TEXT NOT NULL,
                        primaryColor TEXT NOT NULL,
                        secondaryColor TEXT NOT NULL,
                        defaultLanguage TEXT NOT NULL,
                        logoUri TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS products_services (
                        id TEXT NOT NULL PRIMARY KEY,
                        workspaceId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        price REAL NOT NULL,
                        currency TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS business_goals (
                        id TEXT NOT NULL PRIMARY KEY,
                        workspaceId TEXT NOT NULL,
                        goalType TEXT NOT NULL,
                        description TEXT NOT NULL,
                        priority INTEGER NOT NULL,
                        isActive INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS customers (
                        id TEXT NOT NULL PRIMARY KEY,
                        workspaceId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        companyName TEXT,
                        email TEXT,
                        phone TEXT,
                        whatsapp TEXT,
                        industry TEXT,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT INTO workspaces (id, ownerUserId, name, businessProfileId, createdAt, updatedAt, isActive)
                    VALUES ('${WorkspaceDefaults.DEFAULT_WORKSPACE_ID}', '${WorkspaceDefaults.LEGACY_OWNER_USER_ID}', 'My Business', NULL, $now, $now, 1)
                    """.trimIndent()
                )

                db.execSQL(
                    "ALTER TABLE generated_documents ADD COLUMN workspaceId TEXT NOT NULL DEFAULT '${WorkspaceDefaults.DEFAULT_WORKSPACE_ID}'"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai4biz.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

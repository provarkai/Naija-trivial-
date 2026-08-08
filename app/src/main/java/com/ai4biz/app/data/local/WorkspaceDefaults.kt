package com.ai4biz.app.data.local

/**
 * Fixed IDs used both by [AppDatabase]'s MIGRATION_1_2 (raw SQL, runs on
 * upgrade from a pre-Phase-2 install) and by
 * [com.ai4biz.app.data.repository.WorkspaceRepository.getOrCreateDefaultWorkspace]
 * (Kotlin, runs on every app start including fresh installs) -- using the
 * same constant in both places means a migrated device and a fresh install
 * converge on the same default workspace id instead of two different
 * random UUIDs.
 */
object WorkspaceDefaults {
    const val DEFAULT_WORKSPACE_ID = "default-workspace"

    /**
     * Placeholder owner for workspaces created before any real per-user
     * identity existed. [com.ai4biz.app.data.repository.DeviceIdentityRepository]
     * generates a random per-device id for new workspaces going forward;
     * this constant only backfills the one row the migration seeds.
     */
    const val LEGACY_OWNER_USER_ID = "legacy-local-user"
}

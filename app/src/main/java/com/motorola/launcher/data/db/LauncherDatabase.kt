package com.motorola.launcher.data.db

import android.content.Context

abstract class LauncherDatabase {

    abstract fun appDao(): AppDao
    abstract fun waterIntakeDao(): WaterIntakeDao
    abstract fun hydrationSettingsDao(): HydrationSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        fun getInstance(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SqliteLauncherDatabase.getInstance(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

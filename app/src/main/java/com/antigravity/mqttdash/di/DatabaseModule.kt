package com.antigravity.mqttdash.di

import android.content.Context
import androidx.room.Room
import com.antigravity.mqttdash.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mqtt_dash.db"
        ).build()
    }

    @Provides
    fun provideBrokerDao(db: AppDatabase) = db.brokerDao()

    @Provides
    fun provideDashboardDao(db: AppDatabase) = db.dashboardDao()

    @Provides
    fun provideWidgetDao(db: AppDatabase) = db.widgetDao()
}

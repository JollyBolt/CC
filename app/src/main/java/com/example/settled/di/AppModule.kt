package com.example.settled.di

import android.content.Context
import androidx.room.Room
import com.example.settled.data.local.CardDao
import com.example.settled.data.local.SettledDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SettledDatabase {
        return Room.databaseBuilder(
            context,
            SettledDatabase::class.java,
            "settled_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideCardDao(database: SettledDatabase): CardDao {
        return database.cardDao()
    }
}

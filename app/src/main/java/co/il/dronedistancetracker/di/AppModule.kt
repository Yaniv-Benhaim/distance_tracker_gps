package co.il.dronedistancetracker.di

import android.content.Context
import androidx.room.Room
import co.il.dronedistancetracker.data.TravelDatabase
import co.il.dronedistancetracker.data.other.Constants.DATABASE_TABLE
import co.il.dronedistancetracker.utils.KmlUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun providesKmlUtils() = KmlUtils()

    @Singleton
    @Provides
    fun provideTravelDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        TravelDatabase::class.java,
        DATABASE_TABLE
    )
        .fallbackToDestructiveMigration()
        .build()

    @Singleton
    @Provides
    fun provideRunDao(db: TravelDatabase) = db.getTravelDao()
}
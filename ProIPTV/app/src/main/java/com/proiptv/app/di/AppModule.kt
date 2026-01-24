package com.proiptv.app.di

import android.content.Context
import androidx.room.Room
import com.proiptv.app.data.api.XtreamApiService
import com.proiptv.app.data.local.AppDatabase
import com.proiptv.app.data.local.FavoritesDao
import com.proiptv.app.data.local.PreferencesManager
import com.proiptv.app.data.repository.IPTVRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS  // Changed from BODY to avoid logging huge M3U files
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)   // Increased for large M3U files
            .readTimeout(120, TimeUnit.SECONDS)     // Increased for large M3U files
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideXtreamApiService(retrofit: Retrofit): XtreamApiService {
        return retrofit.create(XtreamApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "proiptv_database"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideFavoritesDao(database: AppDatabase): FavoritesDao {
        return database.favoritesDao()
    }
    
    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
    
    @Provides
    @Singleton
    fun provideIPTVRepository(
        apiService: XtreamApiService,
        okHttpClient: OkHttpClient,
        favoritesDao: FavoritesDao
    ): IPTVRepository {
        return IPTVRepository(apiService, okHttpClient, favoritesDao)
    }
}

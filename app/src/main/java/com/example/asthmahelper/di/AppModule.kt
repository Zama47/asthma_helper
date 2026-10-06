package com.example.asthmahelper.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.asthmahelper.data.api.WeatherApiService
import com.example.asthmahelper.data.db.AppDatabase
import com.example.asthmahelper.data.repository.BreathingNormRepositoryImpl
import com.example.asthmahelper.data.repository.DutyMeasurementRepositoryImpl
import com.example.asthmahelper.data.repository.MedicineRepositoryImpl
import com.example.asthmahelper.data.repository.MedicationLogRepositoryImpl
import com.example.asthmahelper.data.repository.MedicationScheduleRepositoryImpl
import com.example.asthmahelper.domain.repository.BreathingNormRepository
import com.example.asthmahelper.domain.repository.DutyMeasurementRepository
import com.example.asthmahelper.domain.repository.MedicineRepository
import com.example.asthmahelper.domain.repository.MedicationLogRepository
import com.example.asthmahelper.domain.repository.MedicationScheduleRepository
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

/** DataStore с настройками погоды (выбранный город). Один экземпляр на процесс. */
private val Context.weatherDataStore by preferencesDataStore(name = "weather_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "asthma_helper_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideDutyMeasurementRepository(db: AppDatabase): DutyMeasurementRepository {
        return DutyMeasurementRepositoryImpl(db.dutyMeasurementDao())
    }

    @Provides
    @Singleton
    fun provideMedicineRepository(db: AppDatabase): MedicineRepository {
        return MedicineRepositoryImpl(db.medicineDao())
    }

    @Provides
    @Singleton
    fun provideMedicationScheduleRepository(db: AppDatabase): MedicationScheduleRepository {
        return MedicationScheduleRepositoryImpl(db.medicationScheduleDao())
    }

    @Provides
    @Singleton
    fun provideMedicationLogRepository(db: AppDatabase): MedicationLogRepository {
        return MedicationLogRepositoryImpl(db.medicationLogDao())
    }

    @Provides
    @Singleton
    fun provideBreathingNormRepository(db: AppDatabase): BreathingNormRepository {
        return BreathingNormRepositoryImpl(db.breathingNormDao())
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApiService(retrofit: Retrofit): WeatherApiService {
        return retrofit.create(WeatherApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWeatherDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.weatherDataStore
    }
}
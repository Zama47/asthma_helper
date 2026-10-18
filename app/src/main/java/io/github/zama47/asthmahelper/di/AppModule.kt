package io.github.zama47.asthmahelper.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.zama47.asthmahelper.data.api.WeatherApiService
import io.github.zama47.asthmahelper.data.db.ALL_MIGRATIONS
import io.github.zama47.asthmahelper.data.db.AppDatabase
import io.github.zama47.asthmahelper.data.repository.ActResultRepositoryImpl
import io.github.zama47.asthmahelper.data.repository.AsthmaAttackLogRepositoryImpl
import io.github.zama47.asthmahelper.data.repository.BreathingNormRepositoryImpl
import io.github.zama47.asthmahelper.data.repository.DutyMeasurementRepositoryImpl
import io.github.zama47.asthmahelper.data.repository.MedicationLogRepositoryImpl
import io.github.zama47.asthmahelper.data.repository.MedicationScheduleRepositoryImpl
import io.github.zama47.asthmahelper.data.repository.MedicineRepositoryImpl
import io.github.zama47.asthmahelper.domain.repository.ActResultRepository
import io.github.zama47.asthmahelper.domain.repository.AsthmaAttackLogRepository
import io.github.zama47.asthmahelper.domain.repository.BreathingNormRepository
import io.github.zama47.asthmahelper.domain.repository.DutyMeasurementRepository
import io.github.zama47.asthmahelper.domain.repository.MedicationLogRepository
import io.github.zama47.asthmahelper.domain.repository.MedicationScheduleRepository
import io.github.zama47.asthmahelper.domain.repository.MedicineRepository
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** DataStore с настройками погоды (выбранный город). Один экземпляр на процесс. */
private val Context.weatherDataStore by preferencesDataStore(name = "weather_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "asthma_helper_db"
    )
        .addMigrations(*ALL_MIGRATIONS)
        .build()

    @Provides
    @Singleton
    fun provideAsthmaAttackLogRepository(db: AppDatabase): AsthmaAttackLogRepository =
        AsthmaAttackLogRepositoryImpl(db.asthmaAttackLogDao())

    @Provides
    @Singleton
    fun provideDutyMeasurementRepository(db: AppDatabase): DutyMeasurementRepository =
        DutyMeasurementRepositoryImpl(db.dutyMeasurementDao())

    @Provides
    @Singleton
    fun provideMedicineRepository(db: AppDatabase): MedicineRepository = MedicineRepositoryImpl(db.medicineDao())

    @Provides
    @Singleton
    fun provideMedicationScheduleRepository(db: AppDatabase): MedicationScheduleRepository =
        MedicationScheduleRepositoryImpl(db.medicationScheduleDao())

    @Provides
    @Singleton
    fun provideMedicationLogRepository(db: AppDatabase): MedicationLogRepository =
        MedicationLogRepositoryImpl(db.medicationLogDao())

    @Provides
    @Singleton
    fun provideBreathingNormRepository(db: AppDatabase): BreathingNormRepository =
        BreathingNormRepositoryImpl(db.breathingNormDao())

    @Provides
    @Singleton
    fun provideActResultRepository(db: AppDatabase): ActResultRepository = ActResultRepositoryImpl(db.actResultDao())

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideWeatherApiService(retrofit: Retrofit): WeatherApiService = retrofit.create(WeatherApiService::class.java)

    @Provides
    @Singleton
    fun provideWeatherDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.weatherDataStore
}

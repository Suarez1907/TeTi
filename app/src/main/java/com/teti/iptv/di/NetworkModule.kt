package com.teti.iptv.di

import com.teti.iptv.data.local.AppPreferences
import com.teti.iptv.data.remote.RatingApiService
import com.teti.iptv.data.remote.XtreamApiService
import com.teti.iptv.data.remote.parser.StreamingM3uParser
import com.teti.iptv.data.remote.parser.StreamingXmltvParser
import com.teti.iptv.data.remote.parser.StreamingXtreamParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(preferences: AppPreferences): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", preferences.userAgent.value)
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://localhost/") // Base URL dinamik olarak URL parametreleriyle ezilir
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideRatingApiService(retrofit: Retrofit): RatingApiService {
        return retrofit.create(RatingApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideStreamingM3uParser(): StreamingM3uParser = StreamingM3uParser()

    @Provides
    @Singleton
    fun provideStreamingXtreamParser(): StreamingXtreamParser = StreamingXtreamParser()

    @Provides
    @Singleton
    fun provideStreamingXmltvParser(): StreamingXmltvParser = StreamingXmltvParser()
}

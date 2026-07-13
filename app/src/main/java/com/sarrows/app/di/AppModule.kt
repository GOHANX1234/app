package com.sarrows.app.di

import android.content.Context
import com.sarrows.app.data.native.NativeSecurity
import com.sarrows.app.data.remote.SarrowsApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext ctx: Context): Context = ctx

    @Provides
    @Singleton
    fun provideNativeSecurity(@ApplicationContext context: Context): NativeSecurity {
        return NativeSecurity(context).also { it.init() }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideSarrowsApiClient(
        nativeSecurity: NativeSecurity,
        okHttpClient: OkHttpClient
    ): SarrowsApiClient = SarrowsApiClient(nativeSecurity, okHttpClient)
}

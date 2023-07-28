package com.maxrave.simpmusic.di

import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.api.search.SearchService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchServiceModule {

    @Provides
    @Singleton
    @Named("LoggingInterceptor")
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return httpLoggingInterceptor
    }
    @Provides
    @Singleton
    @Named("NetworkInterceptor")
    fun provideNetworkInterceptor(): Interceptor {
        val interceptor = Interceptor { chain ->
            val requestBuilder: Request.Builder = chain.request().newBuilder()
            requestBuilder.header("Content-Type", "application/json")
            chain.proceed(requestBuilder.build())
        }
        return interceptor
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(@Named("LoggingInterceptor") httpLoggingInterceptor: HttpLoggingInterceptor, @Named("NetworkInterceptor") networkInterceptor: Interceptor): OkHttpClient {
        val okHttpClient = OkHttpClient.Builder().addInterceptor(httpLoggingInterceptor).addNetworkInterceptor(networkInterceptor).readTimeout(15, TimeUnit.SECONDS).connectTimeout(15, TimeUnit.SECONDS)
        return okHttpClient.build()
    }

    @Provides
    @Singleton
    fun provideGsonConverterFactory(): GsonConverterFactory {
        return GsonConverterFactory.create()
    }
    @Singleton
    @Provides
    @Named("baseSearchUrl")
    fun provideBaseUrl() = Config.BASE_URL

    @Singleton
    @Provides
    @Named("SearchRetrofit")
    fun provideSearchRetrofit(okHttpClient: OkHttpClient, gsonConverterFactory: GsonConverterFactory, @Named("baseSearchUrl") baseUrl: String) : Retrofit
        = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(gsonConverterFactory)
        .client(okHttpClient)
        .build()

    @Singleton
    @Provides
    fun provideSearchService(@Named("SearchRetrofit") retrofit: Retrofit): SearchService = retrofit.create(SearchService::class.java)
}
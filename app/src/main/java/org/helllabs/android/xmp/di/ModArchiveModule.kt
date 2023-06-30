package org.helllabs.android.xmp.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import nl.adaptivity.xmlutil.serialization.XML
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.helllabs.android.xmp.api.ApiHelper
import org.helllabs.android.xmp.api.ApiHelperImpl
import org.helllabs.android.xmp.api.ApiService
import org.helllabs.android.xmp.core.Constants
import retrofit2.Retrofit
@Module
@InstallIn(ViewModelComponent::class)
object ModArchiveModule {

    @ViewModelScoped
    @Provides
    fun provideOkHttpClient() = OkHttpClient.Builder().build()

    @ViewModelScoped
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/xml;".toMediaType()
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(
                XML {
                    autoPolymorphic = true
                }.asConverterFactory(contentType)
            )
            .client(okHttpClient)
            .build()
    }

    @ViewModelScoped
    @Provides
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @ViewModelScoped
    @Provides
    fun provideApiHelper(apiHelper: ApiHelperImpl): ApiHelper = apiHelper
}

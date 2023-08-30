package org.helllabs.android.xmp.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import nl.adaptivity.xmlutil.serialization.XML
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.helllabs.android.xmp.api.ApiHelper
import org.helllabs.android.xmp.api.ApiHelperImpl
import org.helllabs.android.xmp.api.ApiService
import org.helllabs.android.xmp.core.Constants
import retrofit2.Retrofit

interface ModArchiveModule {
    val okHttpClient: OkHttpClient
    val retrofit: Retrofit
    val apiService: ApiService
    val apiHelper: ApiHelper
}

class ModArchiveModuleImpl(
    val appContext: Context
) : ModArchiveModule {

    override val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    override val retrofit: Retrofit by lazy {
        val contentType = "application/xml;".toMediaType()
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(
                XML {
                    autoPolymorphic = true
                }.asConverterFactory(contentType)
            )
            .client(okHttpClient)
            .build()
    }

    override val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    override val apiHelper: ApiHelper by lazy {
        ApiHelperImpl(apiService)
    }
}

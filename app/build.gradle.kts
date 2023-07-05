plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint") version "11.4.2"
    kotlin("kapt")
    kotlin("plugin.serialization") version "1.8.21"
}

android {
    namespace = "org.helllabs.android.xmp"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.helllabs.android.xmp"
        minSdk = 23
        targetSdk = 27

        versionCode = 87
        versionName = "4.12.0"

        ndk.moduleName = "xmp-prebuilt"
        vectorDrawables.useSupportLibrary = true

        ndk.abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

        // ModArchive API Key
        // Must be in your global gradle.properties. ex: C:\Users\<name>\.gradle
        val apiKey = project.property("modArchiveApiKey") as String
        buildConfigField("String", "API_KEY", apiKey)
    }

    buildFeatures {
        aidl = true
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = true
        }
    }

    externalNativeBuild.ndkBuild {
        path = file("src/main/jni/Android.mk")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.7"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    kapt {
        correctErrorTypes = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    // Android support libs.
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.media:media:1.6.0")

    // https://developer.android.com/jetpack/compose/bom/bom-mapping
    val composeBom = platform("androidx.compose:compose-bom:2023.06.01")
    implementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // https://mvnrepository.com/artifact/com.google.accompanist/accompanist-systemuicontroller
    val accompanist = "0.31.5-beta"
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanist")
    implementation("com.google.accompanist:accompanist-permissions:$accompanist")

    // https://mvnrepository.com/artifact/com.github.alorma/compose-settings-storage-preferences
    val settings = "0.27.0"
    implementation("com.github.alorma:compose-settings-storage-preferences:$settings")
    implementation("com.github.alorma:compose-settings-ui-m3:$settings")

    // https://mvnrepository.com/artifact/me.saket.cascade/cascade-compose
    implementation("me.saket.cascade:cascade-compose:2.2.0")

    // https://mvnrepository.com/artifact/androidx.preference/preference-ktx
    implementation("androidx.preference:preference-ktx:1.2.0")

    // https://mvnrepository.com/artifact/com.jakewharton.timber/timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // https://mvnrepository.com/artifact/com.squareup.retrofit2/retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // https://mvnrepository.com/artifact/io.github.pdvrieze.xmlutil/core-android
    val xmlUtil = "0.86.0"
    implementation("io.github.pdvrieze.xmlutil:core-android:$xmlUtil")
    implementation("io.github.pdvrieze.xmlutil:serialization-android:$xmlUtil")

    // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // https://mvnrepository.com/artifact/com.jakewharton.retrofit/retrofit2-kotlinx-serialization-converter
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // https://mvnrepository.com/artifact/com.google.dagger/hilt-android
    implementation("com.google.dagger:hilt-android:2.46.1")
    kapt("com.google.dagger:hilt-android-compiler:2.46.1")

    // https://mvnrepository.com/artifact/com.squareup.leakcanary/leakcanary-android
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}

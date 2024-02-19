@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.kotlin)
    alias(libs.plugins.gradle.ktLint)
    kotlin("kapt")
    kotlin("plugin.serialization") version libs.versions.androidKotlin.get()
}

android {
    namespace = "org.helllabs.android.xmp"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.helllabs.android.xmp"

        /**
         * @see https://apilevels.com/
         */
        minSdk = 23 // Android 6 - Marshmallow
        targetSdk = 34 // Android 14 - Upside Down Cake

        versionCode = 100
        versionName = "5.0"

        vectorDrawables.useSupportLibrary = true

        ndk.abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        externalNativeBuild.cmake.arguments += listOf(
            "-DCMAKE_BUILD_TYPE=Release",
            "-DBUILD_SHARED=OFF"
        )

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
            isJniDebuggable = true
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
            // isEnable = true
            isUniversalApk = true
        }
    }

    // ./gradlew updateLintBaseline
    lint {
        baseline = file("lint-baseline.xml")
    }

    externalNativeBuild.cmake {
        path = file("src/main/jni/CMakeLists.txt")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompiler.get()
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
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
    debugImplementation(libs.leakcanary.android)
    debugImplementation(libs.compose.ui.tooling.preview)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.compose.utils)

    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")

    implementation(libs.dfc)
    implementation(libs.moshi.kotlin)
    implementation(libs.bundles.retrofit)
    implementation(libs.bundles.xmlUtils)
    implementation(libs.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.media)
    implementation(libs.okhttp)
    implementation(libs.preference.ktx)
    implementation(libs.timber)
}

@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.kotlin)
    alias(libs.plugins.gradle.kotlinter)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compiler)
}

android {
    namespace = "org.helllabs.android.xmp"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.helllabs.android.xmp"

        /*
         * https://apilevels.com/
         */
        minSdk = 23 // Android 6 - Marshmallow
        targetSdk = 34 // Android 14 - Upside Down Cake

        versionCode = 103
        versionName = "5.0-SNAPSHOT"

        vectorDrawables.useSupportLibrary = true

        ndk.abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        externalNativeBuild.cmake.arguments += listOf(
            "-DCMAKE_BUILD_TYPE=Release", // DEBUG
            "-DBUILD_SHARED=OFF"
        )

        // ModArchive API Key
        // Must be in your global gradle.properties. ex: C:\Users\<name>\.gradle
        val apiKey = project.property("modArchiveApiKey") as String
        buildConfigField("String", "API_KEY", apiKey)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("GHA") {
            keyAlias = System.getenv("RELEASE_KEYSTORE_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            storeFile = file("keystore.jks")
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
        }
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
        create("GHA") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("GHA")
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

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    composeCompiler {
        enableStrongSkippingMode = true
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

    implementation("io.github.theapache64:rebugger:1.0.0-rc03")

    implementation(libs.bundles.retrofit)
    implementation(libs.bundles.xmlUtils)
    implementation(libs.core.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.dfc)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.media)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.reorderable)
    implementation(libs.timber)
}

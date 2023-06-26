plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint") version "11.4.2"
}

android {
    namespace = "org.helllabs.android.xmp"
    compileSdk = 33

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

    externalNativeBuild.ndkBuild {
        path = file("src/main/jni/Android.mk")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.7"
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    val composeBom = platform("androidx.compose:compose-bom:2023.05.01")
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

    val accompanist = "0.31.4-beta"
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanist")
    implementation("com.google.accompanist:accompanist-permissions:$accompanist")

    // https://mvnrepository.com/artifact/com.github.alorma/compose-settings-storage-preferences
    val settings = "0.27.0"
    implementation("com.github.alorma:compose-settings-storage-preferences:$settings")
    implementation("com.github.alorma:compose-settings-ui-m3:$settings")

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // https://mvnrepository.com/artifact/com.android.volley/volley
    implementation("com.android.volley:volley:1.2.1")

    // https://mvnrepository.com/artifact/com.telly/groundy
    implementation("com.telly:groundy:1.5")

    // https://mvnrepository.com/artifact/androidx.cardview/cardview
    implementation("androidx.cardview:cardview:1.0.0")

    // https://mvnrepository.com/artifact/androidx.recyclerview/recyclerview
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // https://mvnrepository.com/artifact/com.h6ah4i.android.widget.advrecyclerview/advrecyclerview
    implementation("com.h6ah4i.android.widget.advrecyclerview:advrecyclerview:1.0.0")

    // https://mvnrepository.com/artifact/com.fnp/material-preferences
    implementation("com.fnp:material-preferences:1.0.0")

    // https://mvnrepository.com/artifact/androidx.swiperefreshlayout/swiperefreshlayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // https://mvnrepository.com/artifact/androidx.preference/preference-ktx
    implementation("androidx.preference:preference-ktx:1.2.0")

    // https://mvnrepository.com/artifact/com.jakewharton.timber/timber
    implementation("com.jakewharton.timber:timber:5.0.1")
}

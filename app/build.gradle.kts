plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "org.openmw"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alpha3.launcher"
        minSdk = 24

        targetSdk = 35
        versionCode = 2
        versionName = "2.6"

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a", "x86", "x86_64")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                          "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                          "proguard-rules.pro"
            )
            isJniDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
            //keepDebugSymbols += "**/*.so"
        }
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
    dependenciesInfo {
        includeInApk = true
        includeInBundle = true
    }
    ndkVersion = "27.2.12479018"
    buildToolsVersion = "35.0.0"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.javalite)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.runner)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.guava)
    implementation(libs.androidx.webkit)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.gson)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.core.ktx)
    implementation(libs.reorderable)
    implementation(libs.relinker)
    implementation(libs.androidx.window)
    implementation(libs.colorpicker.compose)
    implementation(libs.apache.commons.compress)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.mosaic.runtime)
    implementation(libs.core)
    implementation(libs.termux.app)
    implementation(libs.xz)
    implementation(libs.org.eclipse.jgit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mimokotlin.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mimokotlin.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    flavorDimensions += "engine"
    productFlavors {
        create("standard") {
            dimension = "engine"
        }
        create("geckoview") {
            dimension = "engine"
            minSdk = 26
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "mimo123"
            keyAlias = System.getenv("KEY_ALIAS") ?: "mimo"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "mimo123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.webkit:webkit:1.9.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}

configurations.named("standardImplementation") {
    resolutionStrategy {
        // Standard flavor uses system WebView — keep old AndroidX to avoid Kotlin 2.x metadata.
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
        force("androidx.core:core:1.13.1")
        force("androidx.core:core-ktx:1.13.1")
        force("androidx.annotation:annotation:1.7.1")
        force("androidx.collection:collection:1.4.0")
        force("androidx.activity:activity:1.8.2")
        force("androidx.activity:activity-ktx:1.8.2")
        force("androidx.fragment:fragment:1.6.2")
        force("androidx.fragment:fragment-ktx:1.6.2")
        force("androidx.lifecycle:lifecycle-runtime:2.7.0")
        force("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
        force("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
        force("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
        force("androidx.lifecycle:lifecycle-common:2.7.0")
    }
}

afterEvaluate {
    dependencies {
        add("geckoviewImplementation", "org.mozilla.geckoview:geckoview-arm64-v8a:152.0.20260629141727")
    }
}

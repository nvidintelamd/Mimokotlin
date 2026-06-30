plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mimokotlin.app"
    compileSdk = 34

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

    kotlinOptions {
        jvmTarget = "17"
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

afterEvaluate {
    dependencies {
        add("geckoviewImplementation", "org.mozilla.geckoview:geckoview-arm64-v8a:152.0.20260629141727")
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.diabad"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.diabad"
        minSdk = 31
        targetSdk = 35
        versionCode = 28
        versionName = "0.5.21"
    }

    signingConfigs {
        create("upload") {
            storeFile = rootProject.file("signing/diabad-upload.jks")
            storePassword = "diabad-upload"
            keyAlias = "diabad"
            keyPassword = "diabad-upload"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("upload")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("upload")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.wearable)

    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.protolayout.material)
    implementation(libs.androidx.wear.protolayout.expression)
    implementation(libs.androidx.wear.watchface.complications.datasource.ktx)
    implementation(libs.guava)
}

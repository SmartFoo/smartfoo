plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.google.android.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    implementation(project(":smartfoo-android-lib-core"))
}

android {
    namespace = "com.smartfoo.android.testapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartfoo.android.testapp"
        minSdk = 34
        targetSdk = 35

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    /*
    kotlinOptions {
        jvmTarget = "21"
    }
    */
    buildFeatures {
        viewBinding = true
    }
}

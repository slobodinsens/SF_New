plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.sf_new"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sf_new"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation (libs.androidx.core.ktx)
    implementation (libs.androidx.appcompat.v161)
    implementation (libs.material.v190)
    implementation (libs.androidx.constraintlayout.v214)
    implementation (libs.play.services.maps.v1802)


    implementation (libs.androidx.camera.lifecycle.v130)
    implementation (libs.androidx.camera.view.v130)
    implementation (libs.androidx.camera.camera2.v130)
    implementation (libs.androidx.camera.core.v130)
    implementation(libs.okhttp)

    testImplementation (libs.junit)
    androidTestImplementation (libs.androidx.junit.v115)
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")

}
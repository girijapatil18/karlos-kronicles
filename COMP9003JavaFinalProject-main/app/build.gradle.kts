plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.campusescape"

    // FIXED SDK VERSION
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.campusescape"

        minSdk = 24
        targetSdk = 35

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core)

    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.23.0")
    androidTestImplementation("org.mockito:mockito-android:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
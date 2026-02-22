plugins {
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "1.9.20"
}

android {
    namespace = "com.example.hybridai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.hybridai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            cmake {
                // llama.cpp requires C++17
                cppFlags += "-std=c++17"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
        ndk {
            // Build for modern 64-bit ARM devices (covers 99% of phones sold after 2016)
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // 1.5.5 is required for Kotlin 1.9.20 compatibility
        kotlinCompilerExtensionVersion = "1.5.5"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    // Link CMake to build our llama.cpp shared library
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    // Core Android/Kotlin
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose User Interface ("Senior Designer" Theme)
    // BOM 2023.10.01 ships Compose Compiler 1.5.5 — aligned with Kotlin 1.9.20
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // The Official Google Engine (LLM Hub equivalent)
    // implementation("com.google.mediapipe:tasks-genai:0.10.14")
    
    // Networking for "Maid" Online/Offline Switching
    // implementation("io.ktor:ktor-client-core:2.3.8")
    // implementation("io.ktor:ktor-client-cio:2.3.8")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Navigation Compose — for Chat ↔ Settings navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // DataStore Preferences — persistent settings (API key, theme, model path)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ViewModel for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Icons extended (for Settings, Download icons)
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

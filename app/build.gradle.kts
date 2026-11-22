import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.sllauncher.pro"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // NOTE: the applicationId is permanent once published on Google Play.
        // Confirm it before the first store upload (Phase 20).
        applicationId = "com.sllauncher.pro"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        // CI passes -PversionCode=<run number> so every build can be installed over the last one.
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Release signing uses YOUR upload key. It is read from environment variables (CI secrets)
        // or from keystore.properties (git-ignored). Keys are never stored in the repository.
        create("release") {
            val props = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
                Properties().apply { file.inputStream().use { load(it) } }
            }
            fun value(env: String, prop: String): String? = System.getenv(env) ?: props?.getProperty(prop)
            value("SL_KEYSTORE_PATH", "storeFile")?.let { path ->
                storeFile = file(path)
                storePassword = value("SL_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = value("SL_KEY_ALIAS", "keyAlias")
                keyPassword = value("SL_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Signed only when a release key is configured; otherwise the APK is unsigned.
            signingConfigs.getByName("release").takeIf { it.storeFile != null }?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    // Room writes each database version's schema here, so migrations can be verified.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Local storage: Room for structured data, DataStore for settings.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Battery-aware background work (news refresh, history cleanup).
    implementation(libs.androidx.work.runtime)

    // Local music playback with a system media session.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    // Network images (news thumbnails) with memory/disk caching.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

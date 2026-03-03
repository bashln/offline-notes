plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val appVersionCode = 2
val appVersionName = "0.2.0"

val releaseKeystorePath: String? = System.getenv("OFFLINENOTES_KEYSTORE_PATH")
val releaseKeystorePassword: String? = System.getenv("OFFLINENOTES_KEYSTORE_PASSWORD")
val releaseKeyAlias: String? = System.getenv("OFFLINENOTES_KEY_ALIAS")
val releaseKeyPassword: String? = System.getenv("OFFLINENOTES_KEY_PASSWORD")

val isReleaseTaskRequested: Boolean = gradle.startParameter.taskNames.any { taskName ->
    val lower = taskName.lowercase()
    lower.contains("release") || lower.contains("bundle")
}

android {
    namespace = "com.offlinenotes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.offlinenotes"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        create("release") {
            if (releaseKeystorePath != null) {
                storeFile = file(releaseKeystorePath)
            }
            storePassword = releaseKeystorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                    "OfflineNotes-v$appVersionName+$appVersionCode-release.apk"
            }
        }
    }
}

if (isReleaseTaskRequested) {
    val missing = buildList {
        if (releaseKeystorePath.isNullOrBlank()) add("OFFLINENOTES_KEYSTORE_PATH")
        if (releaseKeystorePassword.isNullOrBlank()) add("OFFLINENOTES_KEYSTORE_PASSWORD")
        if (releaseKeyAlias.isNullOrBlank()) add("OFFLINENOTES_KEY_ALIAS")
        if (releaseKeyPassword.isNullOrBlank()) add("OFFLINENOTES_KEY_PASSWORD")
    }

    if (missing.isNotEmpty()) {
        throw GradleException(
            "Missing release signing env vars: ${missing.joinToString()}. " +
                "Set them before running release tasks."
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("com.google.android.material:material:1.12.0")

    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
}

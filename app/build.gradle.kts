import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"
}

// Release signing credentials. Kept out of the repo: put them in keystore.properties
// (gitignored) or supply them as environment variables in CI. When neither is present
// the release variant simply builds unsigned, so a fresh clone still works.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
        Properties().apply {
            if (keystorePropertiesFile.exists()) {
                keystorePropertiesFile.inputStream().use { load(it) }
            }
        }

fun signingValue(key: String, env: String): String? =
        keystoreProperties.getProperty(key) ?: System.getenv(env)

val releaseStoreFile = signingValue("storeFile", "BEETLE_KEYSTORE_FILE")
val releaseStorePassword = signingValue("storePassword", "BEETLE_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "BEETLE_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "BEETLE_KEY_PASSWORD")

val hasReleaseSigning =
        releaseStoreFile != null &&
                releaseStorePassword != null &&
                releaseKeyAlias != null &&
                releaseKeyPassword != null &&
                file(releaseStoreFile).exists()

android {
    namespace = "com.bsbeetle.bananaspaghetti"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bsbeetle.bananaspaghetti"
        minSdk = 26
        targetSdk = 36
        // Bump versionCode on every build you hand to anyone — Android refuses to install an
        // APK whose versionCode is not greater than the installed one, and UpdatesService has
        // nothing to compare against otherwise.
        versionCode = 2
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // for now the support is only for arm64-v8a [Might add other architectures later]
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        // For the native code for PTY
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }

    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // R8 shrinking is on so the keep rules in proguard-rules.pro are exercised by every
            // release build rather than discovered to be wrong during launch week. The rules cover
            // JNI (pty/lsp), the WebView JS bridges, kotlinx.serialization, Ktor/Netty, JGit and
            // JSch — all of which fail at runtime rather than build time when stripped.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        // CrashReporter stamps reports with the app version, so BuildConfig must be generated.
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.tools.core)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.compose.animation)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.4")

    // For Constraint layout
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")

    // Keyboard
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")

    // File Handling
    implementation("androidx.documentfile:documentfile:1.1.0")

    // to replace
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // ktor for websockets
    implementation("io.ktor:ktor-server-core:3.2.2")
    implementation("io.ktor:ktor-server-cio:3.2.2")
    implementation("io.ktor:ktor-server-websockets:3.2.2")

    val ktorVersion = "3.3.1"
    val kotlinxSerializationVersion = "1.7.3"
    val logbackVersion = "1.4.11"

    implementation("io.ktor:ktor-server-core-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-netty-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-websockets-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${ktorVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${kotlinxSerializationVersion}")
    implementation("org.slf4j:slf4j-android:1.7.36")

    implementation("io.github.kyant0:backdrop:1.0.0")


    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")


    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-svg:2.5.0")

    // Ktor Client
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation(libs.jgit)
    implementation(libs.jgit.ssh.jsch)
    implementation(libs.jsch)
    implementation(libs.androidx.security.crypto)
}
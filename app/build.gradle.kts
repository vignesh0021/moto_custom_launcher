plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.motorola.launcher"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.motorola.launcher"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Room SQLite FTS5 Compiler Schema Options
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
                arguments["room.incremental"] = "true"
                arguments["room.expandProjection"] = "true"
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "dummyPassword"
            keyAlias = System.getenv("KEY_ALIAS") ?: "motoKey"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "dummyPassword"
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseKeystore = file(System.getenv("KEYSTORE_PATH") ?: "keystore.jks")
            if (releaseKeystore.exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Do not silently sign release with debug keys; produce unsigned APK
                signingConfig = null
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

composeCompiler {
    // Enable Compose Compiler Metrics and Reports for CI Gate
    metricsDestination.set(layout.buildDirectory.dir("reports/compose/metrics"))
    reportsDestination.set(layout.buildDirectory.dir("reports/compose/reports"))
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose-stability.conf"))
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose 1.12 (BOM 2026.08.00)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Coroutines & Immutable Collections
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)

    // Room SQLite FTS5 Virtual Table DB
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    annotationProcessor(libs.androidx.room.compiler)

    // Baseline Profiles & ProfileInstaller
    implementation(libs.androidx.profileinstaller)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

tasks.withType<Test>().configureEach {
    // Sanitize unquoted java.library.path containing spaces on Windows hosts
    systemProperty("java.library.path", "")
}


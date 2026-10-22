plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.motorola.launcher.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 34
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // Benchmark requires non-debuggable target
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.uiautomator)
    implementation(libs.junit)
}


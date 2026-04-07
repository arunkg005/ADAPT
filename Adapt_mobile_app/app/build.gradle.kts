plugins {
    alias(libs.plugins.android.application)
}

val apiBaseUrl = (project.findProperty("ADAPT_API_BASE_URL") as String?)
    ?: "http://10.0.2.2:3001/api/"
val releaseStoreFile = project.findProperty("ADAPT_RELEASE_STORE_FILE") as String?
val releaseStorePassword = project.findProperty("ADAPT_RELEASE_STORE_PASSWORD") as String?
val releaseKeyAlias = project.findProperty("ADAPT_RELEASE_KEY_ALIAS") as String?
val releaseKeyPassword = project.findProperty("ADAPT_RELEASE_KEY_PASSWORD") as String?
val hasReleaseSigning = !releaseStoreFile.isNullOrBlank()
        && !releaseStorePassword.isNullOrBlank()
        && !releaseKeyAlias.isNullOrBlank()
        && !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.example.adapt"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.adapt"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["usesCleartextTraffic"] = "false"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }

        release {
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            isMinifyEnabled = false
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

    buildFeatures {
        buildConfig = true
    }
}

tasks.register("verifyReleaseSigning") {
    group = "verification"
    description = "Verifies release signing properties and keystore path are configured."

    doLast {
        if (!hasReleaseSigning) {
            throw GradleException(
                "Release signing is not fully configured. Set ADAPT_RELEASE_STORE_FILE, ADAPT_RELEASE_STORE_PASSWORD, ADAPT_RELEASE_KEY_ALIAS, and ADAPT_RELEASE_KEY_PASSWORD."
            )
        }

        val keystoreFile = file(releaseStoreFile!!)
        if (!keystoreFile.exists()) {
            throw GradleException("Release keystore file not found: ${keystoreFile.absolutePath}")
        }

        println("Release signing configuration is valid.")
    }
}

tasks.register("checkReleaseReadiness") {
    group = "verification"
    description = "Runs signing verification and builds release APK/AAB artifacts."
    dependsOn("verifyReleaseSigning", "assembleRelease", "bundleRelease")
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // WorkManager
    implementation(libs.work.runtime)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

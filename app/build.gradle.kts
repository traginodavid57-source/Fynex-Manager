import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.fynex.manager"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.fynex.manager"
        minSdk = 26
        targetSdk = 35
        versionCode = 100
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keyBase64 = (findProperty("KEY") as? String) ?: System.getenv("KEY")
            val keyDir = rootProject.file("key")
            val keystoreFile = when {
                !keyBase64.isNullOrBlank() -> {
                    keyDir.mkdirs()
                    val decodedFile = File(keyDir, "release.jks")
                    try {
                        val decodedBytes = Base64.getDecoder().decode(keyBase64.trim())
                        decodedFile.writeBytes(decodedBytes)
                        decodedFile
                    } catch (_: Exception) {
                        null
                    }
                }
                rootProject.file("key/release.keystore").exists() -> rootProject.file("key/release.keystore")
                rootProject.file("key/release.jks").exists() -> rootProject.file("key/release.jks")
                else -> null
            }

            val storePass = (findProperty("KEYSTORE_PASSWORD") as? String) ?: System.getenv("KEYSTORE_PASSWORD")
            val alias = (findProperty("KEY_ALIAS") as? String) ?: System.getenv("KEY_ALIAS")
            val keyPass = (findProperty("KEY_PASSWORD") as? String) ?: System.getenv("KEY_PASSWORD")

            if (keystoreFile != null && keystoreFile.exists() && !storePass.isNullOrBlank() && !alias.isNullOrBlank()) {
                storeFile = keystoreFile
                storePassword = storePass
                keyAlias = alias
                keyPassword = if (!keyPass.isNullOrBlank()) keyPass else storePass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseConfig = signingConfigs.findByName("release")
            if (releaseConfig?.storeFile != null && releaseConfig.storeFile!!.exists() && !releaseConfig.storePassword.isNullOrBlank()) {
                signingConfig = releaseConfig
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    applicationVariants.all {
        outputs.all {
            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputImpl.outputFileName = "Fynex-v${defaultConfig.versionName}-${buildType.name}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }

    lint {
        checkReleaseBuilds = false
        disable += "AarMetadataVersionCheck"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.coil)
    implementation(libs.zip4j)
    implementation(libs.topjohnwu.libsu.core)
    implementation(libs.topjohnwu.libsu.io)
    implementation(libs.bcpkix.jdk18on)
    implementation(libs.gson)

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.fragment:fragment-ktx:1.8.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

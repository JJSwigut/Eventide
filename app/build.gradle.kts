import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.sqldelight)
    id("eventide.android.application")
    id("eventide.android.application.compose")
    id("eventide.quality")
}
android {
    val keyStorePropertiesFile = rootProject.file("app/keystore.properties")
    val hasReleaseSigningConfig = keyStorePropertiesFile.exists()

    signingConfigs {
        if (hasReleaseSigningConfig) {
            val keyStoreProperties = loadProperties(keyStorePropertiesFile.absolutePath)
            create("release") {
                storePassword = keyStoreProperties.getProperty("storePassword")
                keyPassword = keyStoreProperties.getProperty("keyPassword")
                keyAlias = keyStoreProperties.getProperty("keyAlias")
                storeFile = file(keyStoreProperties.getProperty("storeFile"))
            }
        }
    }
    namespace = "com.jjswigut.eventide"
    buildTypes {
        getByName("release") {
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(libs.bundles.koinDI)
    implementation(libs.bundles.ktorNetworking)
    implementation(libs.bundles.sqlDelight)
    implementation(libs.bundles.workManager)
    implementation(libs.bundles.dataStore)

    // Use OkHttp engine instead of deprecated Android engine
    implementation("io.ktor:ktor-client-okhttp:${libs.versions.ktor.get()}")

    testImplementation(libs.bundles.test)
}

sqldelight {
    databases {
        create("StationsDb") {
            packageName.set("com.jjswigut.eventide")
        }
    }
}

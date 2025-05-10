import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.sqldelight)
    id("eventide.android.application")
    id("eventide.android.application.compose")
    id("eventide.quality")
}
android {
    signingConfigs {
        val keyStoreProperties = loadProperties("app/keystore.properties")
        create("release") {
            storePassword = keyStoreProperties.getProperty("storePassword")
            keyPassword = keyStoreProperties.getProperty("keyPassword")
            keyAlias = keyStoreProperties.getProperty("keyAlias")
            storeFile = file(keyStoreProperties.getProperty("storeFile"))
        }
    }
    namespace = "com.jjswigut.eventide"
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation(libs.bundles.koinDI)
    implementation(libs.bundles.ktorNetworking)
    implementation(libs.bundles.sqlDelight)

    testImplementation(libs.bundles.test)
}

sqldelight {
    databases {
        create("StationsDb") {
            packageName.set("com.jjswigut.eventide")
        }
    }
}
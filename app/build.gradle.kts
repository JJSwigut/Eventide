@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.android)
    id("stonks.android.application")
    id("stonks.android.application.compose")
    id("stonks.quality")
}
android {
    namespace = "com.jjswigut.eventide"
}

dependencies {
    implementation(libs.bundles.koinDI)
    implementation(libs.bundles.ktorNetworking)

    testImplementation(libs.bundles.test)
}
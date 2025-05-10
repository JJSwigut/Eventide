plugins {
    `kotlin-dsl`
}

group = "com.jjswigut.eventide"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.sqldelight.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplicationCompose") {
            id = "eventide.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "eventide.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("quality") {
            id = "eventide.quality"
            implementationClass = "QualityConventionPlugin"
        }
        register("db") {
            id = "eventide.db"
            implementationClass = "DbConventionPlugin"
        }
    }
}

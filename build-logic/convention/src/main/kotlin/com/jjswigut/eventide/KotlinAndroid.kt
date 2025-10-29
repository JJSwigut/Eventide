package com.jjswigut.eventide

import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.BuildType
import com.android.build.api.dsl.CommonExtension
import com.jjswigut.eventide.EventideBuildType.debug
import com.jjswigut.eventide.EventideBuildType.release
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Configure base Kotlin with Android options
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = ConfigConstants.COMPILE_SDK

        defaultConfig {
            minSdk = ConfigConstants.MIN_SDK
        }

        buildTypes {
            getByName("release") {
                configureBuildType(release)
                proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            }
            getByName("debug") {
                configureBuildType(debug)
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    // Configure Kotlin compiler options using the new API
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    dependencies {
        add(ConfigConstants.IMPLEMENTATION, libraries.findBundle("androidX").get())
    }
}

private fun BuildType.configureBuildType(type: EventideBuildType) {
    this as ApplicationBuildType
    isDebuggable = type.isDebuggable
    isMinifyEnabled = type.isMinifyEnabled
    isCrunchPngs = type.isCrunchPngs
}

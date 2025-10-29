package com.jjswigut.eventide

import com.android.build.api.dsl.CommonExtension
import java.io.File
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Configure Compose-specific options
 */
internal fun Project.configureAndroidCompose(
  commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
  commonExtension.apply {
    buildFeatures {
      compose = true
    }

    composeOptions {
      kotlinCompilerExtensionVersion =
        libraries.findVersion("composeCompiler").get().toString()
    }

    dependencies {
      add(ConfigConstants.IMPLEMENTATION, libraries.findBundle("composeUI").get())
      add(ConfigConstants.IMPLEMENTATION, libraries.findBundle("composeMap").get())
    }
  }

  // Configure Kotlin compiler options using the new API
  extensions.configure<KotlinAndroidProjectExtension> {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
      freeCompilerArgs.addAll(buildComposeMetricsParameters())
    }
  }
}

/**
 * Taken from NowInAndroid, this function configures the Compose compiler to generate reports in
 * the build folder that can help diagnose performance and stability issues within compose.
 */
private fun Project.buildComposeMetricsParameters(): List<String> {
  val metricParameters = mutableListOf<String>()
  val enableMetricsProvider = project.providers.gradleProperty("enableComposeCompilerMetrics")
  val enableMetrics = (enableMetricsProvider.orNull == "true")
  if (enableMetrics) {
    val metricsFolder = File(project.layout.buildDirectory.asFile.get(), "compose-metrics")
    metricParameters.add("-P")
    metricParameters.add(
      "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
        metricsFolder.absolutePath
    )
  }

  val enableReportsProvider = project.providers.gradleProperty("enableComposeCompilerReports")
  val enableReports = (enableReportsProvider.orNull == "true")
  if (enableReports) {
    val reportsFolder = File(project.layout.buildDirectory.asFile.get(), "compose-reports")
    metricParameters.add("-P")
    metricParameters.add(
      "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
        reportsFolder.absolutePath
    )
  }
  return metricParameters.toList()
}

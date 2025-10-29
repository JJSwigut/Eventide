package com.jjswigut.eventide

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * The property extends the Project class in Gradle, providing a direct way to access the
 * VersionCatalog named "libs" from anywhere in the build script.
 */
val Project.libraries: VersionCatalog
    get() = this.extensions.getByType<VersionCatalogsExtension>().named("libs")

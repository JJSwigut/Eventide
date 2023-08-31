package com.jjswigut.stonks

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
/**
* The property extends the Project class in Gradle, providing a direct way to access the
* VersionCatalog named "libs" from anywhere in the build script.
*/
val Project.libraries: VersionCatalog
    get() = this.extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * A function that extends the CommonExtension class to configure Kotlin JVM options.
 *
 * This provides a neat and concise way to specify JVM options for Kotlin tasks.
 * The block parameter is a lambda with KotlinJvmOptions receiver,
 * meaning you can call methods of KotlinJvmOptions directly within the lambda.
 *
 * @param block The configuration block to apply to the Kotlin JVM options.
 *
 *  (copied from NowInAndroid)
 */
fun CommonExtension<*, *, *, *, *>.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
    (this as ExtensionAware).extensions.configure("kotlinOptions", block)
}

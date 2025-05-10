import com.android.build.api.dsl.ApplicationExtension
import com.jjswigut.eventide.ConfigConstants
import com.jjswigut.eventide.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig {
                    applicationId = "com.jjswigut.eventide"
                    minSdk = ConfigConstants.MIN_SDK
                    targetSdk = ConfigConstants.TARGET_SDK
                    // Allow overriding versionCode from gradle properties, default to 9 if not provided
                    versionCode = (project.findProperty("overrideVersionCode")?.toString()?.toIntOrNull()) ?: 9
                    versionName = "2.01"

                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                }
            }
        }
    }
}

import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager){
                apply("com.diffplug.spotless")
                apply("io.gitlab.arturbosch.detekt")
            }

            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("**/*.kt")
                    ktlint("0.46.1")
                    trimTrailingWhitespace()
                    indentWithSpaces()
                    endWithNewline()
                }
                kotlinGradle {
                    target("*.gradle.kts")
                    ktlint()
                }
                format("kts") {
                    target("**/*.kts")
                    targetExclude("**/build/**/*.kts")
                }
            }

            extensions.configure<DetektExtension> {
                config.from("$rootDir/config/detekt/detekt.yml")
                parallel = true
            }
        }
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

kotlin {
    jvmToolchain(17)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint().editorConfigOverride(
            mapOf(
                "ktlint_standard_property-naming" to "disabled",
                "ktlint_standard_function-naming" to "disabled",
            ),
        )
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.konsist)
}

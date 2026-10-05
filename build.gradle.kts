plugins {
    alias(libs.plugins.android.application) apply false
    // Never applied (AGP 9 compiles Kotlin itself). Declaring it lifts the Kotlin Gradle plugin AGP uses
    // from its bundled 2.2.10 to the catalog version, which the Compose compiler plugin must match.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

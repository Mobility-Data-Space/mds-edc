plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.core.spi)
    implementation(libs.edc.core.lib)

    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgres)
}

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.boot.spi)
    implementation(libs.edc.sql.lib)
    implementation(libs.edc.transaction.datasource.spi)

    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgres)

    testImplementation(libs.edc.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(libs.postgres)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}

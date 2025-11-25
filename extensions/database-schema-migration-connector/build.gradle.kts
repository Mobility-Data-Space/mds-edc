plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(libs.edc.core.spi)
    implementation(libs.edc.sql.lib)
    implementation(libs.flyway.core)
    implementation(libs.postgres)
    runtimeOnly(libs.flyway.database.postgres)

    testImplementation(libs.edc.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)

    // TODO: deprecated, could be removed in the next versions
    testImplementation(libs.tractusx.edc.postgresql.migration)
    testImplementation(libs.tractusx.edc.data.plane.migration)
    testImplementation(libs.tractusx.edc.control.plane.migration)
}

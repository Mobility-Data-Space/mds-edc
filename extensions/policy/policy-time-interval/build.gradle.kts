plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.catalog.spi)
    implementation(libs.edc.control.plane.spi)
    implementation(libs.edc.json.ld.spi)
    implementation(libs.edc.participant.spi)
    implementation(libs.edc.policy.engine.spi)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
}

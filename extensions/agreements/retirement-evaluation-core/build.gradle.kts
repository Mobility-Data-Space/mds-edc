plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
}

dependencies {

    implementation(libs.edc.control.plane.spi)
    implementation(libs.edc.core.spi)
    implementation(libs.edc.control.plane.lib)
    implementation(libs.edc.core.lib)
    api(project(":extensions:agreements:retirement-evaluation-spi"))

    testImplementation(libs.assertj)
    testImplementation(libs.mockito.core)
    testImplementation(libs.edc.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)

    testFixturesImplementation(libs.assertj)
    testFixturesImplementation(libs.edc.junit)
    testFixturesImplementation(libs.junit.jupiter)
    testFixturesImplementation(libs.junit.platform.launcher)
}

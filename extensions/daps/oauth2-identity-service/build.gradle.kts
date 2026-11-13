plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.core.spi)
    api(project(":extensions:oauth2-spi"))
    api(libs.edc.control.plane.spi)
    implementation(libs.edc.core.lib)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(testFixtures(libs.edc.core.lib))
    testImplementation(testFixtures(project(":tests")))
}



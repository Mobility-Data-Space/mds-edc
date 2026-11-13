plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.control.plane.spi)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
}

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.core.spi)

    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
}

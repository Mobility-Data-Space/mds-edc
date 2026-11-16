plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.control.plane.spi)
    api(libs.edc.core.spi)

    testImplementation(libs.edc.junit)
}

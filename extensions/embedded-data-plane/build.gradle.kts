plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.core.spi)
    api(project(":extensions:data-plane:data-plane-spi"))
    api(libs.edc.transfer.spi)
    api(libs.edc.control.plane.spi)

    testImplementation(libs.edc.junit)
}

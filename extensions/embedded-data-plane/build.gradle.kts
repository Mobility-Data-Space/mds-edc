plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.core.spi)
    api(libs.edc.control.plane.spi)
    api(libs.edc.data.plane.spi)
    api(libs.edc.data.plane.selector.spi)
    api(libs.edc.participant.context.single.spi)
    api(libs.edc.transfer.spi)

    implementation(libs.edc.data.plane.signaling.core)

    testImplementation(libs.edc.junit)
}

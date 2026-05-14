plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.core.spi)
    api(libs.edc.json.ld.spi)
    api(libs.edc.transfer.spi)
    api(libs.edc.transform.spi)

    implementation(libs.edc.control.plane.transfer)

    testImplementation(libs.edc.junit)
}

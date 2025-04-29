plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.contract.spi)
    implementation(libs.edc.control.plane.spi)
    implementation(libs.edc.core.spi)
    implementation(libs.edc.json.ld.spi)
    implementation(libs.edc.transaction.spi)
    implementation(libs.edc.transform.spi)
    implementation(libs.edc.web.spi)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
}

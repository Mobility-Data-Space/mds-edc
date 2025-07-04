plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.contract.spi)
    implementation(libs.edc.control.plane.spi)
    implementation(libs.edc.core.spi)
    implementation(libs.edc.transaction.spi)
    implementation(libs.edc.web.spi)
    implementation(libs.tractusx.edc.retirement.evaluation.core)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
}

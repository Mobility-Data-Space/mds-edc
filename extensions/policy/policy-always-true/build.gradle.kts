plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.catalog.spi)
    implementation(libs.edc.control.plane.spi)
    implementation(libs.edc.policy.spi)
    implementation(libs.edc.policy.engine.spi)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
}

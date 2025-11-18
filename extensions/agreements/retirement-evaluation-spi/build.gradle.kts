plugins {
    `java-library`
}

dependencies {
    api(libs.edc.policy.engine.spi)
    api(libs.edc.core.spi)
    implementation(libs.tractusx.edc.core.spi)

    testImplementation(libs.edc.junit)
}

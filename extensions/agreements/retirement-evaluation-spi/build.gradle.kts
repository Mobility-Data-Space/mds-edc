plugins {
    `java-library`
}

dependencies {
    api(libs.edc.policy.engine.spi)
    api(libs.edc.core.spi)

    testImplementation(libs.edc.junit)
}

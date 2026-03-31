plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.contract.spi)
    api(libs.edc.core.spi)
    api(libs.edc.policy.engine.spi)

    testImplementation(libs.edc.junit)
}

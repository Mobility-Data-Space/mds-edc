plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.asset.api)
    implementation(libs.edc.core.spi)
    implementation(libs.edc.validator.lib)
    implementation(libs.edc.validator.spi)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.json.ld.lib)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
}

plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.asset.api)
    implementation(libs.edc.core.spi)
    implementation(libs.edc.validator.lib)
    implementation(libs.edc.validator.spi)
    implementation(libs.owlapi.distribution)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
}

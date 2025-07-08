plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.swagger)
}

dependencies {
    implementation(libs.edc.contract.spi)
    implementation(libs.edc.control.plane.spi)
    implementation(libs.edc.core.spi)
    implementation(libs.edc.json.ld.spi)
    implementation(libs.edc.transaction.spi)
    implementation(libs.edc.transform.spi)
    implementation(libs.edc.web.spi)

    implementation(libs.swagger.annotations)
    implementation(libs.swagger.jaxrs2.jakarta)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
}

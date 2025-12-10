plugins {
    `java-library`
    alias(libs.plugins.swagger)
}

dependencies {
    api(libs.edc.core.spi)
    api(libs.edc.http.spi)
    api(libs.edc.web.spi)
    api(libs.edc.data.plane.spi)

    implementation(libs.edc.util.lib)
    implementation(libs.swagger.annotations)
    implementation(libs.jakarta.ws.rs.api)

    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.rest.assured)
    testImplementation(testFixtures(libs.edc.jersey.core))
}

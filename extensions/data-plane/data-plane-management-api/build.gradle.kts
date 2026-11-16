plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.swagger)
}

dependencies {
    implementation(project(":extensions:data-plane:data-plane-spi"))
    implementation(libs.edc.control.plane.spi)
    implementation(libs.edc.core.spi)
    implementation(libs.edc.core.lib)

    implementation(libs.swagger.annotations)
    implementation(libs.swagger.jaxrs2.jakarta)

    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.rest.assured)
    testImplementation(testFixtures(libs.edc.jersey.core))
}

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.swagger)
}

dependencies {
    implementation(project(":extensions:agreements:retirement-evaluation-spi"))
    implementation(libs.edc.json.ld.spi)
    implementation(libs.edc.web.spi)
    implementation(libs.tractusx.edc.core.spi)

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

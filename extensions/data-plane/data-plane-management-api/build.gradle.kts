plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.swagger)
}

dependencies {
    implementation(project(":extensions:data-plane:data-plane-spi"))
    implementation(libs.edc.core.spi)
    implementation(libs.edc.transfer.spi)
    implementation(libs.edc.transaction.spi)
    implementation(libs.edc.transform.spi)
    implementation(libs.edc.json.ld.spi)
    implementation(libs.edc.web.spi)
    implementation(libs.edc.management.api.lib)
    implementation(libs.edc.jersey.providers.lib)

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

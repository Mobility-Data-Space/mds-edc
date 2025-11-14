plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.swagger)
}

dependencies {
    implementation(libs.edc.http.spi)
    implementation(libs.edc.web.spi)
    implementation(libs.edc.data.plane.spi)
    implementation(libs.edc.data.plane.util)
    implementation(libs.edc.util.lib)

    implementation(libs.swagger.annotations)
    implementation(libs.swagger.jaxrs2.jakarta)
//    implementation(libs.jakarta.rsApi)

//    testImplementation(project(":extensions:common:http"))
    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
//    testImplementation(libs.jersey.multipart)
    testImplementation(libs.rest.assured)
    testImplementation(testFixtures(libs.edc.jersey.core))
}


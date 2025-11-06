plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.http.spi)
    api(libs.edc.jwt.signer.spi)
    api(libs.edc.oauth2.spi)
    api(libs.edc.protocol.spi)
    implementation(libs.edc.token.lib)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(testFixtures(libs.edc.http.lib))
    testImplementation(testFixtures(project(":tests")))
}



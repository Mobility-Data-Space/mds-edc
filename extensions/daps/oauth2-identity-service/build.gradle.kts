plugins {
    `java-library`
}

dependencies {
    api(libs.edc.http.spi)
    api(libs.edc.oauth2.spi)
    api(libs.edc.jwt.signer.spi)
    implementation(libs.edc.token.lib)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
    testImplementation(testFixtures(libs.edc.http.lib))
    testImplementation(testFixtures(project(":tests")))
}



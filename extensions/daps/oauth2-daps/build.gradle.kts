plugins {
    `java-library`
}

dependencies {
    api(libs.edc.jwt.spi)
    api(libs.edc.token.spi)

    testImplementation(project(":extensions:daps:oauth2-identity-service"))
    testImplementation(libs.assertj)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.edc.junit)
}



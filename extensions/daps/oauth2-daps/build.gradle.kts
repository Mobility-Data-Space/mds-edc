plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.jwt.spi)
    api(libs.edc.token.spi)

    testImplementation(project(":extensions:daps:oauth2-identity-service"))
    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.testcontainers.junit.jupiter)
}



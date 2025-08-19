plugins {
    `java-library`
    jacoco
}

dependencies {
    implementation(libs.edc.core.spi)
    implementation(libs.edc.validator.spi)
    implementation(project(":extensions:kafka:kafka-spi"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
}

tasks.test {
    useJUnitPlatform()
}

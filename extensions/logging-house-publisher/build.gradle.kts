plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":extensions:agreements:retirement-evaluation-spi"))
    implementation(libs.edc.core.spi)
    implementation(libs.logging.house.client)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
}

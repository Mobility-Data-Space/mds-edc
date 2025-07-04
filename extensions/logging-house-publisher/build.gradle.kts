plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":extensions:contract-retirement:contract-retirement-spi"))
    implementation(libs.edc.core.spi)
    implementation(libs.logging.house.client)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
}

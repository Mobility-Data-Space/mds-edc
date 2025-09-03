plugins {
    `java-library`
}

dependencies {
    implementation(project(":extensions:kafka:data-plane-kafka-spi"))
    implementation(libs.edc.core.spi)
    implementation(libs.edc.data.plane.spi)
    implementation(libs.edc.http.spi)
    implementation(libs.edc.jwt.spi)
    implementation(libs.edc.util.lib)
    implementation(libs.edc.validator.spi)
    implementation(libs.kafka.clients)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.keycloak.admin.client)
    testImplementation(libs.mockito.core)
    testImplementation(libs.parsson)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.keycloak)

    testCompileOnly(project(":launchers:connector-inmemory"))
}

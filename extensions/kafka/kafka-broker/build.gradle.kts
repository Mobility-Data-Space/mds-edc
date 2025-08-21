plugins {
    jacoco
}

dependencies {
    implementation(libs.edc.core.spi)
    implementation(libs.edc.data.plane.spi)
    implementation(libs.edc.jwt.spi)
    implementation(libs.edc.transfer.spi)
    implementation(libs.edc.validator.spi)
    implementation(libs.kafka.clients)
    implementation(libs.edc.util.lib)
    implementation(project(":extensions:kafka:kafka-spi"))
    implementation(project(":extensions:kafka:validator-data-address-kafka"))
    implementation(libs.edc.http.spi)

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

tasks.test {
    useJUnitPlatform()
}

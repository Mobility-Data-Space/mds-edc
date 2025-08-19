plugins {
    jacoco
}

dependencies {
    implementation(libs.edc.core.spi)
    implementation(libs.edc.transfer.spi)
    implementation(libs.edc.validator.spi)
    implementation(libs.kafka.clients)
    implementation(libs.edc.util.lib)
    implementation(project(":extensions:kafka:kafka-spi"))
    implementation(project(":extensions:kafka:validator-data-address-kafka"))
    implementation(libs.edc.http.spi)
    implementation(libs.edc.transfer.data.plane.signaling)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
}

tasks.test {
    useJUnitPlatform()
}

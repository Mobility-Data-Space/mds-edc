plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":extensions:data-plane:kafka:data-plane-kafka-spi"))
    implementation(libs.edc.core.spi)
    implementation(project(":extensions:data-plane:data-plane-spi"))
    implementation(libs.edc.core.lib)
    implementation(libs.jose4j)
    implementation(libs.kafka.clients)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.keycloak.admin.client)
    testImplementation(libs.mockito.core)
    testImplementation(libs.parsson)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.keycloak)

    testCompileOnly(project(":launchers:connector-inmemory"))
}

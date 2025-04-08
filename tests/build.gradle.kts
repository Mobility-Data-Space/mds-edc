dependencies {
    testImplementation(libs.edc.boot.lib)
    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(libs.edc.management.api.test.fixtures))
    testImplementation(libs.tractusx.edc.core.spi)

    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.keycloak.admin.client)
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.rest.assured)
    testImplementation(libs.postgres)
    testImplementation(libs.testcontainers.vault)
    testImplementation(libs.testcontainers.postgresql)

    testCompileOnly(project(":launchers:connector-inmemory"))
    testCompileOnly(project(":launchers:connector-vault-postgresql"))
    testCompileOnly(project(":launchers:connector-vault-postgresql-edp"))
}

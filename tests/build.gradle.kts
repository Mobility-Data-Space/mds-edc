dependencies {
    testImplementation(libs.edc.boot.lib)
    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(libs.edc.management.api.test.fixtures))
    testImplementation(libs.tractusx.edc.core.spi)

    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
    testImplementation(libs.aws.iam)
    testImplementation(libs.aws.s3)
    testImplementation(libs.azure.storage.blob)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.keycloak.admin.client)
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.rest.assured)
    testImplementation(libs.postgres)
    testImplementation(libs.testcontainers.localstack)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.vault)

    testCompileOnly(project(":launchers:connector-inmemory"))
    testCompileOnly(project(":launchers:connector-vault-postgresql"))
    testCompileOnly(project(":launchers:connector-vault-postgresql-edp"))
}

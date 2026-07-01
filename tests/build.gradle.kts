plugins {
    `java-test-fixtures`
}

dependencies {
    testImplementation(libs.edc.boot.lib)
    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(libs.edc.management.api.test.fixtures))

    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
    testImplementation(libs.aws.iam)
    testImplementation(libs.aws.s3)
    testImplementation(libs.azure.storage.blob)
    testImplementation(libs.dsp.tck)
    testImplementation(libs.jose4j)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.kafka.clients)
    testImplementation(libs.keycloak.admin.client)
    testImplementation(libs.wiremock)
    testImplementation(libs.rest.assured)
    testImplementation(libs.postgres)
    testImplementation(libs.testcontainers.localstack)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.vault)

    testCompileOnly(project(":launchers:connector-inmemory"))
    testCompileOnly(project(":launchers:connector-inmemory-dcp"))
    testCompileOnly(project(":launchers:connector-tck"))
    testCompileOnly(project(":launchers:connector-vault-postgresql"))
    testCompileOnly(project(":launchers:connector-vault-postgresql-dcp"))
    testCompileOnly(project(":launchers:connector-vault-postgresql-edp"))
    testCompileOnly(project(":launchers:wallet"))

    testFixturesApi(libs.bouncycastle.bcpkix)
    testFixturesApi(libs.nimbus.jwt)
}

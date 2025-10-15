plugins {
    `java-test-fixtures`
}

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
    testImplementation(libs.dsp.tck.boot)
    testImplementation(libs.dsp.tck.core)
    testImplementation(libs.dsp.tck.runtime)
    testImplementation(libs.dsp.tck.system)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.kafka.clients)
    testImplementation(libs.keycloak.admin.client)
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.rest.assured)
    testImplementation(libs.postgres)
    testImplementation(libs.testcontainers.localstack)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.vault)

    testRuntimeOnly(libs.dsp.tck.metadata)
    testRuntimeOnly(libs.dsp.tck.catalog)
    testRuntimeOnly(libs.dsp.tck.contract.negotiation)
    testRuntimeOnly(libs.dsp.tck.transfer.process)

    testCompileOnly(project(":launchers:connector-inmemory"))
    testCompileOnly(project(":launchers:connector-tck"))
    testCompileOnly(project(":launchers:connector-vault-postgresql"))
    testCompileOnly(project(":launchers:connector-vault-postgresql-edp"))

    testFixturesApi(libs.bouncycastle.bcpkix)
}

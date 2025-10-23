plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(libs.edc.issuerservice.bom)
    runtimeOnly(libs.edc.issuerservice.feature.sql.bom)
    runtimeOnly(libs.edc.issuerservice.database.attestations)
    runtimeOnly(libs.edc.vault.hashicorp)

    implementation(project(":extensions:identity-hub:super-user-seeder"))
    implementation(project(":extensions:identity-hub:database-schema-migration-issuer"))
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

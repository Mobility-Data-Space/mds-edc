plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    implementation(project(":launchers:base-connector"))

    implementation(project(":extensions:dcp"))
    runtimeOnly(libs.edc.controlplane.dcp.bom)

    runtimeOnly(libs.edc.vault.hashicorp)

    runtimeOnly(libs.edc.controlplane.feature.sql.bom)
    runtimeOnly(libs.edc.dataplane.feature.sql.bom)

    implementation(project(":extensions:agreements:retirement-evaluation-store-sql"))
    implementation(project(":extensions:database-schema-migration-connector"))

    runtimeOnly(libs.logging.house.client)

}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

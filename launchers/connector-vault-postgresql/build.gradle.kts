plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

configurations.all {
    exclude(group = "org.eclipse.edc", module = "control-api-configuration")
    exclude(group = "org.eclipse.edc", module = "control-plane-api")
    exclude(group = "org.eclipse.edc", module = "data-plane-selector-control-api")
    exclude(group = "org.eclipse.edc", module = "data-plane-signaling-api")
}

dependencies {
    implementation(project(":launchers:base-connector"))

    implementation(project(":extensions:daps:oauth2-daps"))
    implementation(project(":extensions:daps:oauth2-identity-service"))
    runtimeOnly(libs.edc.vault.hashicorp)

    runtimeOnly(libs.edc.controlplane.feature.sql.bom)

    implementation(project(":extensions:agreements:retirement-evaluation-store-sql"))
    implementation(project(":extensions:database-schema-migration-connector"))

    implementation(project(":extensions:data-plane:sql"))

    runtimeOnly(libs.logging.house.client)

}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

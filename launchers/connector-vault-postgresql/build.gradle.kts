plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(project(":launchers:launcher-base"))

    implementation(project(":extensions:daps:oauth2-daps"))
    implementation(project(":extensions:daps:oauth2-identity-service"))
    runtimeOnly(libs.edc.vault.hashicorp)

    runtimeOnly(libs.edc.controlplane.feature.sql.bom)
    runtimeOnly(libs.edc.dataplane.feature.sql.bom)

    runtimeOnly(libs.tractusx.edc.postgresql.migration)
    runtimeOnly(libs.tractusx.edc.data.plane.migration)
    runtimeOnly(libs.tractusx.edc.control.plane.migration)

    runtimeOnly(libs.tractusx.edc.retirement.evaluation.store.sql)

    runtimeOnly(libs.logging.house.client)

}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

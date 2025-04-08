plugins {
    application
    distribution
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(project(":launchers:connector-inmemory"))

    runtimeOnly(libs.edc.oauth2.daps)
    runtimeOnly(libs.edc.oauth2.service)
    runtimeOnly(libs.edc.vault.hashicorp)

    runtimeOnly(libs.edc.controlplane.feature.sql.bom)
    runtimeOnly(libs.edc.dataplane.feature.sql.bom)

    // Tractusx EDC migrations libraries
    runtimeOnly(libs.tractusx.edc.postgresql.migration)
    runtimeOnly(libs.tractusx.edc.data.plane.migration)
    runtimeOnly(libs.tractusx.edc.control.plane.migration)

    runtimeOnly(libs.tractusx.edc.retirement.evaluation.store.sql)

    runtimeOnly(project(":extensions:edp"))
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

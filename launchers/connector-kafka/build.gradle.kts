    plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(project(":launchers:connector-inmemory")) {
        // TODO(upstream): currently there's no way around this, it will disable EDR for HTTP-Data, only here for the sake of Kafka solution
        exclude("org.eclipse.edc", "data-plane-iam")
    }

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

    implementation(project(":extensions:kafka:data-plane-kafka"))
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

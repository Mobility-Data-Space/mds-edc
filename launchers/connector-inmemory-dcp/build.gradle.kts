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
    implementation(project(":extensions:dcp"))
    runtimeOnly(libs.edc.controlplane.dcp.bom)
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

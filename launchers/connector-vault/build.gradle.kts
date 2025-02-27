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
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

plugins {
    application
    distribution
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(project(":launchers:connector-inmemory"))

    runtimeOnly(libs.edc.vault.hashicorp)
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

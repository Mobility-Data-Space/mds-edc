plugins {
    application
    distribution
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(project(":launchers:connector-vault-postgresql"))

    implementation(project(":extensions:edp"))
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

plugins {
    application
    distribution
    `maven-publish`
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(project(":launchers:connector-vault-postgresql"))

    runtimeOnly(project(":extensions:edp"))
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

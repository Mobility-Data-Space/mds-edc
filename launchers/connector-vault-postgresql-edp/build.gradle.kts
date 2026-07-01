plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    implementation(project(":launchers:connector-vault-postgresql"))

    implementation(project(":extensions:edp"))
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

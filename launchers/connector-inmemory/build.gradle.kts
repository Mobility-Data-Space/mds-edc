plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    implementation(project(":launchers:base-connector"))
    runtimeOnly(libs.edc.iam.mock)
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(libs.edc.identityhub.bom)
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

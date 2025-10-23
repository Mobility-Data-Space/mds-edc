plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(libs.edc.identityhub.bom)
    implementation(project(":extensions:identity-hub:super-user-seeder"))
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

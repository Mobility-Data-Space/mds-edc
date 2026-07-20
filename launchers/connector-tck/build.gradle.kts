plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(libs.edc.controlplane.base.bom)
    runtimeOnly(libs.edc.dataplane.base.bom)

    implementation(project(":extensions:embedded-data-plane"))

    runtimeOnly(libs.edc.iam.mock)
    runtimeOnly(libs.edc.tck.extension)
    runtimeOnly(libs.bouncycastle.bcpkix)

}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

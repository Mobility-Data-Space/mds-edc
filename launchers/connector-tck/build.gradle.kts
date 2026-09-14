plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

configurations.all {
    exclude(group = "org.eclipse.edc", module = "data-plane-signaling-core")
    exclude(group = "org.eclipse.edc", module = "data-plane-signaling-oauth2")
}

dependencies {
    runtimeOnly(libs.edc.controlplane.base.bom)

    runtimeOnly(libs.edc.iam.mock)
    runtimeOnly(libs.edc.tck.extension)
    runtimeOnly(libs.bouncycastle.bcpkix)
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

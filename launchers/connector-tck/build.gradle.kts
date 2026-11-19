plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

configurations.all {
    // no data plane signaling
    exclude(group = "org.eclipse.edc", module = "data-plane-signaling-core")
    exclude(group = "org.eclipse.edc", module = "data-plane-signaling-oauth2")
    exclude(group = "org.eclipse.edc", module = "data-plane-signaling-oauth2-token-exchange")

    // makes the provider send a second TransferStartMessage on RESUMING: TP:01-04 fails
    exclude(group = "eu.dataspace", module = "embedded-data-plane")

    // needs embedded-data-plane
    exclude(group = "eu.dataspace", module = "data-plane")
    exclude(group = "eu.dataspace", module = "data-plane-management-api")
    exclude(group = "eu.dataspace", module = "data-plane-aws-s3")
    exclude(group = "eu.dataspace", module = "data-plane-azure-storage")
}

dependencies {
    implementation(project(":launchers:base-connector"))

    // TCK-only
    runtimeOnly(libs.edc.iam.mock)
    runtimeOnly(libs.edc.tck.extension)
    runtimeOnly(libs.bouncycastle.bcpkix)
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

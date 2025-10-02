plugins {
    `maven-publish`
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(libs.edc.controlplane.base.bom)

    runtimeOnly(libs.edc.dataplane.base.bom) {
        exclude(group = edcGroupId, module = "data-plane-selector-client")
    }

    runtimeOnly(libs.edc.data.plane.public.api.v2) // this has been deprecated, but it will be provided by tractus-x edc starting from version 0.10.0

    runtimeOnly(libs.edc.aws.data.plane.aws.s3)
    runtimeOnly(libs.edc.aws.validator.data.address.s3)
    runtimeOnly(libs.edc.azure.data.plane.azure.storage)

    runtimeOnly(libs.tractusx.edc.retirement.evaluation.api)
    runtimeOnly(libs.tractusx.edc.retirement.evaluation.core)

    implementation(project(":extensions:kafka:data-plane-kafka"))
    implementation(project(":extensions:logging-house-publisher"))
    implementation(project(":extensions:manual-negotiation-approval"))
    implementation(project(":extensions:policy:policy-always-true"))
    implementation(project(":extensions:policy:policy-referring-connector"))
    implementation(project(":extensions:policy:policy-time-interval"))
    implementation(project(":extensions:semantic-validator"))
}

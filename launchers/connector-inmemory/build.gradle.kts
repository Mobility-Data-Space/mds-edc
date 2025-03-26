plugins {
    application
    distribution
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(libs.edc.controlplane.base.bom) {
        exclude(group = edcGroupId, module = "auth-tokenbased")
    }

    runtimeOnly(libs.edc.dataplane.base.bom) {
        exclude(group = edcGroupId, module = "data-plane-selector-client")
    }

    runtimeOnly(libs.edc.data.plane.public.api.v2) // this has been deprecated, but it will be provided by tractus-x edc starting from version 0.10.0

    runtimeOnly(libs.tractusx.edc.retirement.evaluation.api)
    runtimeOnly(libs.tractusx.edc.retirement.evaluation.core)

    runtimeOnly(libs.edc.iam.mock)

    implementation(project(":extensions:policy:policy-always-true"))
    implementation(project(":extensions:policy:policy-referring-connector"))
    implementation(project(":extensions:policy:policy-time-interval"))
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

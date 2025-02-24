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

    runtimeOnly(libs.edc.control.plane.api) // should not be needed anymore since https://github.com/eclipse-edc/Connector/issues/4759 gets resolved
    runtimeOnly(libs.edc.iam.mock)
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

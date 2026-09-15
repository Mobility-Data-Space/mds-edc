plugins {
    `maven-publish`
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(libs.edc.controlplane.base.bom)
    implementation(project(":extensions:data-plane"))
    runtimeOnly(libs.edc.transfer.data.plane.signaling) // legacy signaling protocol

    implementation(project(":extensions:data-plane:aws:data-plane-aws-s3"))
    implementation(project(":extensions:data-plane:aws:validator-data-address-s3"))
    implementation(project(":extensions:data-plane:azure:data-plane-azure-storage"))

    implementation(project(":extensions:agreements:retirement-evaluation-api"))
    implementation(project(":extensions:agreements:retirement-evaluation-core"))
    implementation(project(":extensions:patches"))
    implementation(project(":extensions:embedded-data-plane"))
    implementation(project(":extensions:logging-house-publisher"))
    implementation(project(":extensions:management-jsonld-context"))
    implementation(project(":extensions:manual-negotiation-approval"))
    implementation(project(":extensions:policy:policy-always-true"))
    implementation(project(":extensions:policy:policy-referring-connector"))
    implementation(project(":extensions:policy:policy-time-interval"))
    implementation(project(":extensions:semantic-validator"))
}

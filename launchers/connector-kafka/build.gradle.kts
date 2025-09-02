plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(project(":launchers:connector-vault-postgresql")) {
        // TODO(upstream): currently there's no way around this, it will disable EDR for HTTP-Data, only here for the sake of Kafka solution
        exclude(edcGroupId, "data-plane-iam")
    }

    implementation(project(":extensions:kafka:data-plane-kafka"))
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

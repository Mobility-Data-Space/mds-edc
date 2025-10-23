plugins {
    application
    distribution
    `maven-publish`
    alias(libs.plugins.openapi.generator)
}

val edcGroupId = "org.eclipse.edc"

dependencies {
    runtimeOnly(libs.edc.identityhub.bom)
    runtimeOnly(libs.edc.identityhub.feature.sql.bom)
    runtimeOnly(libs.edc.vault.hashicorp)

    runtimeOnly(libs.edc.participantcontext.config.store.sql) // this can be deleted after bumping EDC to 0.17.x

    implementation(project(":extensions:identity-hub:database-schema-migration-wallet"))
    implementation(project(":extensions:identity-hub:super-user-seeder"))
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

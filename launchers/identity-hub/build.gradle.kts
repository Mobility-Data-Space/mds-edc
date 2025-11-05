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

    implementation(project(":extensions:identity-hub:database-schema-migration-identity-hub"))
    implementation(project(":extensions:identity-hub:super-user-seeder"))
}

application {
    mainClass = "$edcGroupId.boot.system.runtime.BaseRuntime"
}

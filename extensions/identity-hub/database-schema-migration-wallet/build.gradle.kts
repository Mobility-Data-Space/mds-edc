plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.boot.spi)
    implementation(libs.edc.sql.lib)
    implementation(libs.edc.transaction.datasource.spi)

    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgres)
}

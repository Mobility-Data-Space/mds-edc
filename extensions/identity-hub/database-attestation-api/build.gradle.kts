plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.boot.spi)
    implementation(libs.edc.identity.hub.spi)
//    implementation(libs.edc.issuerservice.database.attestations)
    implementation(libs.edc.sql.lib)
    implementation(libs.edc.transaction.datasource.spi)
    implementation(libs.edc.web.spi)

    implementation(libs.swagger.annotations)
    implementation(libs.swagger.jaxrs2.jakarta)
}

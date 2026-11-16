plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))
    api(libs.edc.core.spi)
    api(libs.edc.sql.lease.spi)

    implementation(libs.edc.sql.bootstrapper)
    implementation(libs.edc.sql.lease)
    implementation(libs.edc.core.lib)

    testImplementation(testFixtures(project(":extensions:data-plane:data-plane-spi")))
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))

}



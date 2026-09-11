plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))
    api(libs.edc.core.spi)

    implementation(libs.edc.sql.bootstrapper)
    implementation(libs.edc.sql.lib)
    implementation(libs.edc.util.lib)

    testImplementation(testFixtures(project(":extensions:data-plane:data-plane-spi")))
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
}



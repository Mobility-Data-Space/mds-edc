plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":extensions:data-plane:data-plane-spi"))

    implementation(libs.edc.core.spi)
    implementation(libs.edc.core.lib)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
    testImplementation(testFixtures(libs.edc.junit))
    testImplementation(testFixtures(project(":extensions:data-plane:data-plane-spi")))
}

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":extensions:agreements:retirement-evaluation-spi"))

    implementation(libs.edc.core.spi)
    implementation(libs.edc.transaction.spi)
    implementation(libs.edc.sql.lib)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
    testImplementation(testFixtures(libs.edc.junit))
    testImplementation(testFixtures(project(":extensions:agreements:retirement-evaluation-core")))
}

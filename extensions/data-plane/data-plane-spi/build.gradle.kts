plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(libs.edc.core.spi)
    api(libs.edc.control.plane.spi)

    testFixturesApi(libs.edc.junit)
    testFixturesImplementation(libs.junit.jupiter)
    testFixturesImplementation(libs.awaitility)
    testFixturesImplementation(libs.assertj)
}



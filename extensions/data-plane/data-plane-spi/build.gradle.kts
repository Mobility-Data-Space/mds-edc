plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(libs.edc.core.spi)
    api(libs.edc.data.address.http.data.spi)

    testFixturesApi(libs.edc.junit)
    testFixturesImplementation(libs.junit.jupiter)
    testFixturesImplementation(libs.awaitility)
    testFixturesImplementation(libs.assertj)
}



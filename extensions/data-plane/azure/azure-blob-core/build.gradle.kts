plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(libs.edc.core.spi)
    api(libs.edc.control.plane.spi)
    implementation(libs.azure.storage.blob)
    implementation(libs.azure.identity)
    implementation(libs.edc.util.lib)

    testFixturesApi(libs.edc.data.plane.util)
}



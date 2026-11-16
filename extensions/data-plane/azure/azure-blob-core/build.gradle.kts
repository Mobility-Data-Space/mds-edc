plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(libs.edc.core.spi)
    api(libs.edc.control.plane.spi)
    implementation(libs.azure.storage.blob)
    implementation(libs.azure.identity)
    implementation(libs.edc.core.lib)

    testFixturesApi(project(":extensions:data-plane:data-plane-util"))
}



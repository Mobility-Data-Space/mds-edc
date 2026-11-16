plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:data-plane:azure:azure-blob-core"))
    api(project(":extensions:data-plane:data-plane-spi"))
    api(libs.edc.control.plane.spi)
    implementation(project(":extensions:data-plane:data-plane-util"))
    implementation(libs.edc.core.lib)

    implementation(libs.azure.storage.blob)

    testImplementation(testFixtures(project(":extensions:data-plane:azure:azure-blob-core")))
    testImplementation(libs.testcontainers.junit.jupiter)
}

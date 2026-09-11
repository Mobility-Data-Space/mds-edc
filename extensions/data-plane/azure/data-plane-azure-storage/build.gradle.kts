plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:data-plane:azure:azure-blob-core"))
    api(project(":extensions:data-plane:data-plane-spi"))
    api(libs.edc.participant.context.single.spi)
    implementation(project(":extensions:data-plane:data-plane-util"))
    implementation(libs.edc.util.lib)

    implementation(libs.azure.storage.blob)

    testImplementation(testFixtures(project(":extensions:data-plane:azure:azure-blob-core")))
    testImplementation(libs.edc.json.lib)
    testImplementation(libs.testcontainers.junit.jupiter)
}

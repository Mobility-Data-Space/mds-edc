plugins {
    `java-library`
}

dependencies {
    api(libs.edc.core.spi)
    api(project(":extensions:data-plane:data-plane-spi"))

    implementation(libs.edc.query.lib)
    implementation(libs.edc.store.lib)
    implementation(libs.edc.state.machine.lib)
    implementation(libs.edc.util.lib)
    implementation(project(":extensions:data-plane:data-plane-util"))

    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(libs.awaitility)
    testImplementation(testFixtures(project(":extensions:data-plane:data-plane-spi")))
}



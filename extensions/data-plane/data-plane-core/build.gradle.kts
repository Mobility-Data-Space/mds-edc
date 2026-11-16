plugins {
    `java-library`
}

dependencies {
    api(libs.edc.core.spi)
    api(project(":extensions:data-plane:data-plane-spi"))

    implementation(libs.edc.core.lib)
    implementation(libs.edc.control.plane.lib)
    implementation(project(":extensions:data-plane:data-plane-util"))

    implementation(libs.opentelemetry.instrumentation.annotations)


    testImplementation(libs.awaitility)
    testImplementation(testFixtures(project(":extensions:data-plane:data-plane-spi")))
}

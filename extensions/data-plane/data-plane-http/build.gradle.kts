plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))
    api(libs.edc.core.spi)
    api(libs.edc.http.spi)

    implementation(project(":extensions:data-plane:data-plane-util"))
    implementation(libs.edc.util.lib)

    testImplementation(project(":extensions:data-plane:data-plane-core"))
    testImplementation(libs.edc.runtime.core)
    testImplementation(libs.edc.json.ld)
    testImplementation(libs.rest.assured)
    testImplementation(libs.wiremock)

    testImplementation(testFixtures(project(":extensions:data-plane:data-plane-spi")))
    testImplementation(testFixtures(libs.edc.http.lib))
}

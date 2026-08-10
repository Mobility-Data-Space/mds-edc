plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.core.spi)
    api(libs.edc.control.plane.spi)
    api(libs.edc.data.plane.spi)
    api(libs.edc.http.spi)
    api(libs.edc.participant.context.single.spi)
    implementation(libs.edc.dsp.catalog.transform.lib)

    testImplementation(libs.edc.json.ld.lib)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.assertj)
}

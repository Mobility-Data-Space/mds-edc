plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.core.spi)
    implementation(libs.edc.web.spi)
    implementation(libs.edc.json.ld.lib)
    implementation(libs.edc.control.plane.spi)
    implementation(libs.edc.boot)
 
    implementation(libs.edc.edr.store.spi)
 
    implementation(libs.edc.data.plane.selector.spi)

    implementation(libs.edc.data.plane.http.spi)

    implementation(libs.yasson)
}

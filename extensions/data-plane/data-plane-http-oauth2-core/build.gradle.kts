plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))
    api(libs.edc.core.spi)
    api(libs.edc.oauth2.spi)

    implementation(libs.edc.token.lib)
    implementation(libs.edc.util.lib)

    testImplementation(libs.rest.assured)
}



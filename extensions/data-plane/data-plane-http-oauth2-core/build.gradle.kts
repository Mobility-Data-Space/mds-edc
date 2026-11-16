plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))
    api(project(":extensions:oauth2-spi"))
    api(libs.edc.core.spi)

    implementation(libs.edc.core.lib)

    testImplementation(libs.rest.assured)
}



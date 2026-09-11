plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))
    api(libs.edc.core.spi)

    implementation(libs.edc.token.lib)
    implementation(libs.edc.util.lib)
}



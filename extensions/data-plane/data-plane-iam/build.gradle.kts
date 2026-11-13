plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))
    api(libs.edc.core.spi)

    implementation(libs.edc.core.lib)
}



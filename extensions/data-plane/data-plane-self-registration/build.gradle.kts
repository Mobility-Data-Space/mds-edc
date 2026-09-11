plugins {
    `java-library`
}

dependencies {
    api(libs.edc.core.spi)
    api(libs.edc.control.plane.spi)
    api(libs.edc.data.plane.selector.spi)
    api(libs.edc.web.spi)
}



plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.core.spi)
    api(libs.edc.json.ld.spi)
    api(libs.edc.policy.monitor.spi)
    api(libs.edc.transaction.spi)
    api(libs.edc.transfer.spi)
    api(libs.edc.transform.spi)
    api(libs.edc.web.spi)

    implementation(libs.edc.asset.api)
    implementation(libs.edc.jersey.providers.lib)
    implementation(libs.edc.control.plane.transfer)
    implementation(libs.jakarta.annotation.api)

    testImplementation(libs.edc.junit)
}

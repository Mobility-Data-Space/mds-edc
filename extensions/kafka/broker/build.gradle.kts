plugins {
   jacoco
}

dependencies {
   implementation(libs.edc.transfer.spi)
   implementation(libs.edc.validator.spi)
   implementation(libs.kafka.clients)
   implementation(libs.edc.util.lib)
   implementation(project(":extensions:kafka:data-address-spi"))
   implementation(project(":extensions:kafka:validator-data-address"))
   implementation(libs.edc.http.spi)
   implementation(libs.edc.transfer.data.plane.signaling)

   testImplementation(libs.junit.jupiter)
   testImplementation(libs.assertj)
   testImplementation(libs.edc.junit)
   testImplementation(libs.mockito.core)
}

tasks.test {
   useJUnitPlatform()
}
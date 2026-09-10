plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:data-plane:aws:aws-spi"))
    api(libs.edc.transfer.spi)
    api(libs.edc.validator.spi)

    api(libs.aws.iam)
    api(libs.aws.s3)
    api(libs.aws.sts)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
}



plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":extensions:data-plane:data-plane-core"))
    implementation(project(":extensions:data-plane:data-plane-http"))
    implementation(project(":extensions:data-plane:data-plane-http-oauth2-core"))
    implementation(project(":extensions:data-plane:data-plane-iam"))
    implementation(project(":extensions:data-plane:data-plane-public-api-v2"))
    implementation(project(":extensions:data-plane:data-plane-self-registration"))
    implementation(project(":extensions:data-plane:kafka:data-plane-kafka"))
    implementation(project(":extensions:data-plane:observer"))
}



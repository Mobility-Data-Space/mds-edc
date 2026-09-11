plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":extensions:data-plane:sql:accesstokendata-store-sql"))
    implementation(project(":extensions:data-plane:sql:data-plane-store-sql"))
    implementation(project(":extensions:data-plane:sql:observer-sql"))
}



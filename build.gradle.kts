/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */


plugins {
    `java-library`
}

buildscript {
    dependencies {
        val version: String by project
        classpath("org.eclipse.edc.edc-build:org.eclipse.edc.edc-build.gradle.plugin:$version")
    }
}

allprojects {
    apply(plugin = "${group}.edc-build")

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        pom { // TODO fix repo and company details
            scmUrl.set("https://github.com/OWNER/REPO.git")
            scmConnection.set("scm:git:git@github.com:OWNER/REPO.git")
            developerName.set("yourcompany")
            developerEmail.set("admin@yourcompany.com")
            projectName.set("your cool project based on EDC")
            projectUrl.set("www.coolproject.com")
            description.set("your description")
            licenseUrl.set("https://opensource.org/licenses/MIT")
        }
    }

}

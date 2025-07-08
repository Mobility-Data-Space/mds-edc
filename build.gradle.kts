import io.swagger.v3.plugins.gradle.SwaggerPlugin
import io.swagger.v3.plugins.gradle.tasks.ResolveTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.swagger) apply false
}

allprojects {
    apply(plugin = "java")

    testing {
        suites {
            val test by getting(JvmTestSuite::class) {
                useJUnitJupiter()
            }
        }
    }

    tasks.withType<Test> {
        testLogging {
            events(TestLogEvent.FAILED)
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    // needed for E2E tests
    tasks.register("printClasspath") {
        dependsOn(tasks.compileJava)
        doLast {
            println(sourceSets["main"].runtimeClasspath.asPath)
        }
    }

}

subprojects {

    afterEvaluate {
        if (project.plugins.hasPlugin(SwaggerPlugin::class.java)) {
            tasks.withType<ResolveTask> {
                outputFileName = "openapi"
                outputFormat = ResolveTask.Format.YAML
                classpath = sourceSets.main.get().runtimeClasspath
                buildClasspath = classpath
                resourcePackages.add("eu.dataspace.connector")
            }
        }

        if (project.plugins.hasPlugin(MavenPublishPlugin::class.java)) {
            publishing {
                publications {
                    create<MavenPublication>(project.name) {
                        from(components["java"])
                        groupId = "eu.dataspace.connector"

                        pom {
                            licenses {
                                license {
                                    name = "The Apache License, Version 2.0"
                                    url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                                }
                            }
                        }
                    }
                }

                repositories {
                    maven {
                        url = uri("https://maven.pkg.github.com/Mobility-Data-Space/mds-edc")
                        credentials {
                            username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                            password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
                        }
                    }
                }
            }
        }
    }

}

import io.swagger.v3.plugins.gradle.SwaggerPlugin
import io.swagger.v3.plugins.gradle.tasks.ResolveTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.openapitools.generator.gradle.plugin.OpenApiGeneratorPlugin
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.swagger) apply false
    alias(libs.plugins.openapi.generator) apply false
}

allprojects {
    apply(plugin = "java")

    tasks.test {
        useJUnitPlatform {
            excludeTags("Tck")
        }

        testLogging {
            events(TestLogEvent.FAILED)
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    tasks.register<Test>("tck") {
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath

        useJUnitPlatform {
            includeTags("Tck")
        }

        testLogging {
            showStandardStreams = true
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

    plugins.withType<MavenPublishPlugin> {
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

    afterEvaluate {

        plugins.withType<SwaggerPlugin> {
            tasks.withType<ResolveTask> {
                outputFileName = project.name
                outputDir = project.layout.buildDirectory.dir("openapi").get().asFile
                outputFormat = ResolveTask.Format.YAML
                openApiFile = rootDir.resolve("resources").resolve("openapi-config.yml")
                classpath = sourceSets.main.get().runtimeClasspath
                buildClasspath = classpath
                resourcePackages = setOf<String>("eu.dataspace.connector")
            }
        }

        plugins.withType<OpenApiGeneratorPlugin> {

            tasks.withType<GenerateTask> {
                dependsOn("gatherOpenApi")
                generatorName.set("openapi-yaml")
                inputSpecRootDirectory.set("${layout.buildDirectory.get().asFile}/openapi")
                outputDir.set("${layout.buildDirectory.get().asFile}/generated")
                mergedFileName = project.name
            }
        }

        plugins.withType<DistributionPlugin> {

            tasks.register("gatherOpenApi") {
                val outputDir = project.layout.buildDirectory.dir("openapi")
                outputs.dir(outputDir)

                doLast {
                    val destinationDirectory = outputDir.get().asFile

                    // download from maven repository
                    configurations.asMap.values
                        .asSequence()
                        .filter { it.isCanBeResolved }
                        .map { it.resolvedConfiguration.firstLevelModuleDependencies }.flatten()
                        .map { childrenDependencies(it) }.flatten()
                        .distinct()
                        .forEach { dep ->
                            downloadYamlArtifact(dep, "management-api", destinationDirectory);
                            downloadYamlArtifact(dep, "observability-api", destinationDirectory);
                            downloadYamlArtifact(dep, "public-api", destinationDirectory);
                        }

                    // get internal libraries
                    getAllProjectInternalDependencies(project)
                        .map { it.path.drop(1).replace(":", "/") }
                        .map { File(it) }
                        .mapNotNull { it.resolve("build").resolve("openapi").listFiles() }
                        .flatMap { it.asSequence() }
                        .forEach {
                            it.copyTo(destinationDirectory.resolve(it.name), overwrite = true)
                        }
                }
            }

        }

    }

}

fun getAllProjectInternalDependencies(project: Project): List<ProjectDependency> {
    val projectDependencies = project.configurations
        .filter { !it.name.startsWith("test") }
        .flatMap { it.dependencies }
        .filterIsInstance<ProjectDependency>()


    val inner = projectDependencies.stream().flatMap { projectDependency ->
        allprojects.find { it.path == projectDependency.path }
            ?.let { getAllProjectInternalDependencies(it) }
            ?.stream()
    }.toList()

    return projectDependencies + inner
}

fun childrenDependencies(dependency: ResolvedDependency): List<ResolvedDependency> {
    return listOf(dependency) + dependency.children.map { child -> childrenDependencies(child) }.flatten()
}

fun downloadYamlArtifact(dep: ResolvedDependency, classifier: String, destinationDirectory: File) {
    try {
        val managementApi = dependencies.create(dep.moduleGroup, dep.moduleName, dep.moduleVersion, classifier = classifier, ext = "yaml")
        configurations
            .detachedConfiguration(managementApi)
            .resolve()
            .forEach { file ->
                destinationDirectory
                    .resolve("${dep.moduleName}.yaml")
                    .let(file::copyTo)
            }
    } catch (_: Exception) {
    }
}

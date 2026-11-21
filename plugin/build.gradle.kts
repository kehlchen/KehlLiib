plugins {
    alias(libs.plugins.run.paper)
    `maven-publish`
}

description = "Standalone Paper and Purpur plugin distribution for KehlLib"

base {
    archivesName = "kehllib"
}

dependencies {
    implementation(project(":core"))
    compileOnly(libs.paper.api)
}

tasks {
    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms1G", "-Xmx1G")
    }

    jar {
        dependsOn(":api:jar", ":core:jar")
        from(project(":api").extensions.getByType<SourceSetContainer>()["main"].output)
        from(project(":core").extensions.getByType<SourceSetContainer>()["main"].output)
        from(configurations.runtimeClasspath.get().filter { it.isFile && it.name.endsWith(".jar") }
            .map { zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}

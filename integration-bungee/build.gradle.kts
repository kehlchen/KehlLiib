plugins {
    `maven-publish`
}

description = "Optional BungeeCord plugin messaging integration for KehlLib"

dependencies {
    api(project(":api"))
    compileOnly(libs.paper.api)
}

base {
    archivesName = "kehllib-integration-bungee"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}


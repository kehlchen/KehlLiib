description = "Public APIs and abstractions for KehlLib"

plugins {
    `maven-publish`
}

dependencies {
    compileOnly(libs.paper.api)
}

base {
    archivesName = "kehllib-api"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

plugins {
    `maven-publish`
}

description = "Optional Lettuce-backed Redis integration for KehlLib"

dependencies {
    api(project(":api"))
    implementation("io.lettuce:lettuce-core:6.7.1.RELEASE")
}

base {
    archivesName = "kehllib-integration-redis"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}


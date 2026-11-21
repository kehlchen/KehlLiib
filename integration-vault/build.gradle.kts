plugins {
    `maven-publish`
}

description = "Optional Vault economy provider for KehlLib"

repositories {
    maven("https://jitpack.io")
}

dependencies {
    api(project(":api"))
    compileOnly(libs.paper.api)
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}

base {
    archivesName = "kehllib-integration-vault"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}


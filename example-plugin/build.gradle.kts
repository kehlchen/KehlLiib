plugins {
    alias(libs.plugins.run.paper)
}

description = "Example plugin demonstrating standalone KehlLib integration"

dependencies {
    implementation(project(":core"))
    compileOnly(libs.paper.api)
}

base {
    archivesName = "kehllib-example-plugin"
}

tasks {
    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms1G", "-Xmx1G")
    }
}

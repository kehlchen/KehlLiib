dependencies {
    api(project(":api"))
    compileOnly(libs.paper.api)
    testImplementation(libs.paper.api)
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.yaml:snakeyaml:2.5")
}

plugins {
    `maven-publish`
}

base {
    archivesName = "kehllib-core"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

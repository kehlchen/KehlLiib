plugins {
    `maven-publish`
}

description = "Optional Velocity proxy integration for KehlLib"

dependencies {
    api(project(":api"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}

base {
    archivesName = "kehllib-integration-velocity"
}

tasks {
    jar {
        from(project(":api").extensions.getByType<SourceSetContainer>()["main"].output)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

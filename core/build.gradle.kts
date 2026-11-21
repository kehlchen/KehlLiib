dependencies {
    api(project(":api"))
    compileOnly(libs.paper.api)
    testImplementation(libs.paper.api)
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.yaml:snakeyaml:2.5")
    implementation("com.zaxxer:HikariCP:6.3.0")
    runtimeOnly("org.xerial:sqlite-jdbc:3.50.3.0")
    runtimeOnly("com.mysql:mysql-connector-j:9.3.0")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.5.3")
    runtimeOnly("org.postgresql:postgresql:42.7.5")
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

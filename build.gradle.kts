plugins {
    java
}

group = "cloud.jtheberg"
version = "1.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")

    // Base de données
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.45.0.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.2")
}

tasks {
    jar {
        archiveFileName.set("NeticAI-${version}.jar")

        // Inclure les dépendances dans le JAR
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        manifest {
            attributes["Main-Class"] = "cloud.jtheberg.netic.NeticPlugin"
        }
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
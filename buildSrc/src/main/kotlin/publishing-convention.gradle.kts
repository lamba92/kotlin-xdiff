import kotlin.io.path.Path
import kotlin.io.path.readText

plugins {
    id("org.jetbrains.dokka")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.github.gradle-nexus.publish-plugin")
    `maven-publish`
    signing
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGeneratePublicationHtml)
    archiveClassifier = "javadoc"
    from(tasks.dokkaGeneratePublicationHtml)
    destinationDirectory = layout.buildDirectory.dir("artifacts")
}

publishing {
    repositories {
        maven(rootProject.layout.buildDirectory.dir("mavenRepo")) {
            name = "test"
        }
    }
    publications.withType<MavenPublication> {
        artifact(javadocJar)
        pom {
            name = "kotlin-xdiff"
            description = "Git's xdiff for Kotlin Multiplatform"
            url = "https://github.com/lamba92/kotlin-xdiff"
            licenses {
                license {
                    name = "Apache-2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "lamba92"
                    name = "Lamberto Basti"
                    email = "basti.lamberto@gmail.com"
                }
            }
            scm {
                connection = "https://github.com/lamba92/kotlin-xdiff.git"
                developerConnection = "https://github.com/lamba92/kotlin-xdiff.git"
                url = "https://github.com/lamba92/kotlin-xdiff.git"
            }
        }
    }
}

signing {
    val privateKey =
        System.getenv("SIGNING_PRIVATE_KEY")
            ?: project.properties["central.signing.privateKeyPath"]
                ?.let { it as? String }
                ?.let { Path(it).readText() }
            ?: return@signing
    val password =
        System.getenv("SIGNING_PASSWORD")
            ?: project.properties["central.signing.privateKeyPassword"] as? String
            ?: return@signing
    useInMemoryPgpKeys(privateKey, password)
    sign(publishing.publications)
}

tasks {

    // workaround https://github.com/gradle/gradle/issues/26091
    withType<PublishToMavenRepository> {
        dependsOn(withType<Sign>())
    }
}

nexusPublishing {
    // repositoryDescription is used by the nexus publish plugin as identifier
    // for the repository to publish to.
    val repoDesc =
        System.getenv("SONATYPE_REPOSITORY_DESCRIPTION")
            ?: project.properties["central.sonatype.repositoryDescription"] as? String
    repoDesc?.let { repositoryDescription = it }

    repositories {
        sonatype {
            username = System.getenv("SONATYPE_USERNAME")
                ?: project.properties["central.sonatype.username"] as? String
            password = System.getenv("SONATYPE_PASSWORD")
                ?: project.properties["central.sonatype.password"] as? String
        }
    }
}

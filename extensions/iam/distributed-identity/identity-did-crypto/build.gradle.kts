plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:iam:distributed-identity:identity-did-spi"))

    implementation("org.bouncycastle:bcpkix-jdk15on:1.56")
}

publishing {
    publications {
        create<MavenPublication>("identity-did-crypto") {
            artifactId = "identity-did-crypto"
            from(components["java"])
        }
    }
}

import org.gradle.api.plugins.quality.Pmd

plugins {
    java
    id("org.springframework.boot") version "3.5.11"
    id("io.spring.dependency-management") version "1.1.7"
    pmd
}

group = "id.ac.ui.cs.advprog"
version = "0.0.1-SNAPSHOT"
description = "bidmart-api-gateway"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

extra["springCloudVersion"] = "2025.0.2"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

pmd {
    ruleSets = emptyList()
    isConsoleOutput = true
    toolVersion = "7.11.0"
}

tasks.withType<Pmd>().configureEach {
    ignoreFailures = false
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<Pmd>("pmdMain") {
    ruleSetFiles = files("$rootDir/config/pmd/ruleset.xml")
}

tasks.named<Pmd>("pmdTest") {
    ruleSetFiles = files("$rootDir/config/pmd/ruleset-test.xml")
}

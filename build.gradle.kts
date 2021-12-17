plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.0.0-beta-4"
  kotlin("plugin.spring") version "1.6.0"
  kotlin("plugin.jpa") version "1.6.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.0.3")

  implementation("org.springdoc:springdoc-openapi-ui:1.6.1")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.1")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.1")

  implementation("org.apache.commons:commons-lang3:3.12.0")

  runtimeOnly("com.h2database:h2:2.0.202")
  runtimeOnly("org.flywaydb:flyway-core:8.2.2")
  runtimeOnly("org.postgresql:postgresql:42.3.1")

  testImplementation("org.awaitility:awaitility-kotlin:4.1.1")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  testImplementation("org.mockito:mockito-inline:4.1.0")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.2.0-beta"
  kotlin("plugin.spring") version "1.5.0"
  kotlin("plugin.jpa") version "1.5.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework:spring-jms")
  implementation("org.springframework.cloud:spring-cloud-aws-messaging:2.2.6.RELEASE")

  implementation("org.springdoc:springdoc-openapi-ui:1.5.8")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.5.8")

  implementation(platform("com.amazonaws:aws-java-sdk-bom:1.11.1013"))
  implementation("com.amazonaws:amazon-sqs-java-messaging-lib:1.0.8")
  implementation("org.apache.commons:commons-lang3:3.12.0")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

  runtimeOnly("com.h2database:h2:1.4.200")
  runtimeOnly("org.flywaydb:flyway-core:7.8.2")
  runtimeOnly("org.postgresql:postgresql:42.2.20")

  testImplementation("org.awaitility:awaitility-kotlin:4.0.3")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
}

tasks {
  compileKotlin {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}

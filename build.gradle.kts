plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.1.7"
  kotlin("plugin.spring") version "1.4.32"
  kotlin("plugin.jpa") version "1.4.32"
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

  implementation(platform("com.amazonaws:aws-java-sdk-bom:1.11.1001"))
  implementation("com.amazonaws:amazon-sqs-java-messaging-lib:1.0.8")
  implementation("org.apache.commons:commons-lang3:3.12.0")

  runtimeOnly("com.h2database:h2:1.4.200")
  runtimeOnly("org.flywaydb:flyway-core:7.8.1")
  runtimeOnly("org.postgresql:postgresql:42.2.19")

  testImplementation("org.awaitility:awaitility-kotlin:4.0.3")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
}

tasks {
  compileKotlin {
    kotlinOptions {
      jvmTarget = "15"
    }
  }
}

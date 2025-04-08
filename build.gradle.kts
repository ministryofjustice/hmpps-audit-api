import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.0.0"
  kotlin("plugin.spring") version "2.1.20"
  kotlin("plugin.jpa") version "2.1.20"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
  all {
    resolutionStrategy {
      force("org.apache.commons:commons-configuration2:2.11.0")
    }
  }
}

dependencyCheck {
  suppressionFiles.add("audit-suppressions.xml")
}

ext["hibernate.version"] = "6.5.3.Final"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.2")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

  implementation("org.apache.commons:commons-lang3:3.17.0")
  implementation("software.amazon.awssdk:s3:2.31.17")
  implementation("software.amazon.awssdk:athena:2.31.17")
  implementation("org.apache.parquet:parquet-avro:1.15.1")
  implementation("org.apache.avro:avro:1.12.0")
  implementation("org.apache.hadoop:hadoop-client:3.4.1") {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
    exclude(group = "org.apache.hadoop.thirdparty", module = "hadoop-shaded-protobuf_3_25")
    exclude(group = "dnsjava", module = "dnsjava")
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    exclude(group = "com.google.guava", module = "guava")
    exclude(group = "org.eclipse.jetty", module = "jetty-servlet")
    exclude(group = "org.eclipse.jetty", module = "jetty-webapp")
    exclude(group = "org.eclipse.jetty.websocket", module = "websocket-common")
    exclude(group = "org.eclipse.jetty.websocket", module = "websocket-client")
  }

  runtimeOnly("com.h2database:h2:2.3.232")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.5")

  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  testImplementation("org.mockito:mockito-inline:5.2.0")

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("javax.xml.bind:jaxb-api:2.3.1")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}

tasks.test {
  systemProperty("spring.profiles.active", System.getProperty("spring.profiles.active"))
}

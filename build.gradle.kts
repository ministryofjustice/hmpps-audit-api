import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.3.1"
  kotlin("plugin.spring") version "2.3.21"
  kotlin("plugin.jpa") version "2.3.21"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
  all {
    resolutionStrategy {
      force("org.apache.commons:commons-configuration2:2.11.0")
      force("commons-beanutils:commons-beanutils:1.11.0")
    }
  }
}

dependencyCheck {
  suppressionFiles.add("audit-suppressions.xml")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-webflux")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.3.2")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")
  constraints {
    implementation("org.webjars:swagger-ui:5.32.2")
  }

  implementation("org.apache.commons:commons-lang3:3.20.0")
  implementation("software.amazon.awssdk:s3:2.44.4")
  implementation("software.amazon.awssdk:athena:2.44.4")
  implementation("org.apache.parquet:parquet-avro:1.17.1")
  implementation("org.apache.avro:avro:1.12.1")
  implementation("org.apache.hadoop:hadoop-client:3.5.0") {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
    exclude(group = "org.apache.hadoop.thirdparty", module = "hadoop-shaded-protobuf_3_25")
    exclude(group = "dnsjava", module = "dnsjava")
    exclude(group = "org.jline", module = "jline")
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    exclude(group = "com.google.guava", module = "guava")
    exclude(group = "org.eclipse.jetty", module = "jetty-servlet")
    exclude(group = "org.eclipse.jetty", module = "jetty-webapp")
    exclude(group = "org.eclipse.jetty.websocket", module = "websocket-common")
    exclude(group = "org.eclipse.jetty.websocket", module = "websocket-client")
    exclude(group = "commons-beanutils", module = "commons-beanutils")
  }
  implementation("commons-beanutils:commons-beanutils:1.11.0")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("org.glassfish.jaxb:jaxb-runtime:4.0.7")

  runtimeOnly("com.h2database:h2:2.4.240")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.11")

  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  testImplementation("org.mockito:mockito-inline:5.2.0")

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}

kotlin {
  jvmToolchain(25)
  compilerOptions {
    freeCompilerArgs.addAll("-Xannotation-default-target=param-property")
  }
}

tasks {
  withType<KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }
}

tasks.test {
  systemProperty("spring.profiles.active", System.getProperty("spring.profiles.active"))
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val mockkVersion = "1.12.7"
val logstashVersion = "7.2"
val jsoupVersion = "1.15.3"
val openHtmlToPdfVersion = "1.0.10"
val kotlinxHtmlVersion = "0.7.5"
val springDocVersion = "1.6.11"

repositories {
    mavenCentral()
    jcenter()
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    id("org.springframework.boot") version "2.7.3"
    id("org.jetbrains.kotlin.plugin.spring") version "1.7.10"
    idea
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.springdoc:springdoc-openapi-ui:$springDocVersion")

    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:$openHtmlToPdfVersion")
    implementation("com.openhtmltopdf:openhtmltopdf-slf4j:$openHtmlToPdfVersion")
    implementation("com.openhtmltopdf:openhtmltopdf-svg-support:$openHtmlToPdfVersion")

    implementation("ch.qos.logback:logback-classic")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion")

    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.springframework:spring-mock:2.0.8")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage")
    }
    testImplementation("org.mockito:mockito-inline:4.7.0")
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("app.jar")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src/main/kotlin")

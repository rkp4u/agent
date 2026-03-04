plugins {
    id("java")
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Web (Tomcat + REST support)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // LangChain4j BOM — manages versions of ALL core modules
    implementation(platform("dev.langchain4j:langchain4j-bom:1.1.0"))

    // ✅ Core modules — versions auto-resolved by BOM
    implementation("dev.langchain4j:langchain4j-mcp")
    implementation("dev.langchain4j:langchain4j-open-ai")

    // ✅ Spring Boot Starters with OpenAI autoconfiguration
    implementation("dev.langchain4j:langchain4j-spring-boot-starter:1.1.0-beta7")
    implementation("dev.langchain4j:langchain4j-open-ai-spring-boot-starter:1.1.0-beta7")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

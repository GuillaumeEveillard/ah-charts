plugins {
    kotlin("jvm")
}

group = "org.ggye"
version = "1.0-SNAPSHOT"

val ktor_version = "1.3.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ui"))
    
    implementation(kotlin("stdlib-jdk8"))
    
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.google.guava:guava:29.0-jre")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    implementation("com.xenomachina:kotlin-argparser:2.0.7")
    
    //ktor
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-apache:$ktor_version")
    implementation("io.ktor:ktor-client-auth-basic:$ktor_version")
    implementation("io.ktor:ktor-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-client-json:$ktor_version")
    implementation("io.ktor:ktor-client-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-gson:$ktor_version")
    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version")
    implementation("io.ktor:ktor-client-auth-jvm:$ktor_version")
    
    testImplementation("org.apache.commons:commons-text:1.8")
    testImplementation("org.skyscreamer:jsonassert:1.5.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Jar> {
        manifest {
            attributes["Main-Class"] = "ServerKt"
        }
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
}


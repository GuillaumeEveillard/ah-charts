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
    
    implementation("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    implementation("org.skyscreamer:jsonassert:1.5.0")
    implementation("org.apache.commons:commons-text:1.8")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
//        jar {
//            manifest {
//                attributes["Main-Class"] = "ServerKt"
//            }
////            from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
//        }
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


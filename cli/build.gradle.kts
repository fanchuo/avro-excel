plugins {
    `java-module`
}

dependencies {
    implementation(project(":core"))
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")
}

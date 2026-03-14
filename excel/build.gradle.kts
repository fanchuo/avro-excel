plugins {
    `java-module`
}

dependencies {
    implementation(project(":core"))
    implementation("org.apache.poi:poi-ooxml:5.4.1")
}

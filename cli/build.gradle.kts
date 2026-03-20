plugins {
    `java-module`
    application
}

application {
    mainClass.set("org.fanchuo.avroexcel.cli.AvroExcel")
}

dependencies {
    implementation(project(":excel"))
    implementation(project(":core"))
    runtimeOnly(project(":parquet"))
    implementation("info.picocli:picocli:4.7.6")
    testImplementation(project(":test-files"))
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")
}

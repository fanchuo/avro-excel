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
    implementation(project(":parquet"))
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")
}

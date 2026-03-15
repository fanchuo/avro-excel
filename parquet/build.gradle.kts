plugins {
    `java-module`
}

dependencies {
    implementation(project(":core"))
    implementation("org.apache.parquet:parquet-avro:1.17.0")
    implementation("org.apache.hadoop:hadoop-client:3.3.6")
}

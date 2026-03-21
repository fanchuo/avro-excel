group = "org.fanchuo.avroexcel"
version = "1.0-SNAPSHOT"

allprojects {
    tasks.withType<Jar> {
        archiveBaseName.set("avroexcel-${project.name}")
    }
}
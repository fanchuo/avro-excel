plugins {
    id("base")
    id("jacoco-report-aggregation")
}

repositories {
    mavenCentral()
}

dependencies {
    jacocoAggregation(project(":core"))
    jacocoAggregation(project(":excel"))
    jacocoAggregation(project(":parquet"))
    jacocoAggregation(project(":cli"))
    jacocoAggregation(project(":test-files"))
}

reporting {
    reports {
        val testCodeCoverageReport by creating(JacocoCoverageReport::class) {
            testSuiteName = "test"
        }
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}

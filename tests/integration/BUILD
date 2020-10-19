load("//private/ruls/maven:pom.bzl", "declare_pom")
load("//private/ruls/maven:repository.bzl", "maven_repository")



declare_pom(
    name = "pom=._mvn-parent-pom",
    pom_file = "//tests/integration/mvn-parent-pom:pom.xml"
)


declare_pom(
    name = "pom=._mvn-build-lib-one",
    pom_file = "//tests/integration/mvn-build-lib-one:pom.xml",
    parent = ":pom=._mvn-parent-pom"
)


maven_repository(
    name = "repo",
    deps = [
        "pom=._mvn-build-lib-one",
        "pom=._mvn-parent-pom"
    ]
)
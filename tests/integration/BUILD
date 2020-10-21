load("//private/ruls/maven:repository.bzl", "declare_pom")
load("//private/ruls/maven:repository.bzl", "maven_repository")


declare_pom(
    name = "the_mvn-parent-pom",
    pom_file = "//tests/integration/mvn-parent-pom:pom.xml"
)


declare_pom(
    name = "the_mvn-build-lib-one",
    pom_file = "//tests/integration/mvn-build-lib-one:pom.xml",
    parent = ":the_mvn-parent-pom"
)


maven_repository(
    name = "repo",
    modules = [
        "the_mvn-parent-pom",
        "the_mvn-build-lib-one"
    ]
)
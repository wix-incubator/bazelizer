load("//maven:defs.bzl", "maven_project")

maven_project(
    name = "maven",
    pom_file = ":pom.xml",
    parent_project = "//tests/e2e/mvn-lib-parent:maven",
    flags = [
        "--mvn-extra-args", "install"
    ],
    visibility = ["//visibility:public"],
)


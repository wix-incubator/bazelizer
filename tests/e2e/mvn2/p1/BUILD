load("@maven_e2e_v3//:execute_build.bzl", "execute_build")
load("//private/ruls/maven_v3:maven_project.bzl", "maven_project")

maven_project(
    name = "maven",
    pom_file = ":pom.xml",
    visibility = ["//visibility:public"]
)

execute_build(
    name = "build",
    project = ":maven"
)
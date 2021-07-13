load("//maven:defs.bzl", "maven_project")

load("@maven_e2e_v3//:execute_build.bzl", "execute_build")


maven_project(
    name = "maven",
    pom_file = ":pom.xml",
    parent_project = "//tests/e2e/mvn-lib-subparent:maven",
    visibility = ["//visibility:public"],
)

filegroup(
    name = "sources",
    srcs = glob(["src/**/*"]),
    visibility = ["//visibility:public"]
)

execute_build(
    name = "mvn-lib-module-non-compiled",
    project = ":maven",
    srcs = [":sources"],
    visibility = ["//visibility:public"]
)

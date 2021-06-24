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
    srcs = glob(["src/**/*"])
)

execute_build(
    name = "mvn-lib-module-b",
    project = ":maven",
    deps = [ "//tests/e2e/mvn-lib-subparent/mvn-lib-module-a" ],
    srcs = [":sources"],
    visibility = ["//visibility:public"]
)
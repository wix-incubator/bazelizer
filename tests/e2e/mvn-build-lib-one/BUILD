load("//maven:defs.bzl", "declare_pom")

load("@maven_e2e//:execute_build.bzl", "execute_build")

filegroup(
    name = "sources",
    srcs = glob(["src/**/*"]),
    visibility = ["//visibility:public"]
)


#
# API 2.0
#

declare_pom(
    name = "declared_pom",
    pom_file = ":pom.xml",
    visibility = ["//visibility:public"]
)


execute_build(
    name = "mvn-build-lib-one",
    pom_def = ":declared_pom",
    srcs = [":sources"],
    visibility = ["//visibility:public"]
)
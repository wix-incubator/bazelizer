load("//maven:defs.bzl", "declare_module")

load("@maven_e2e_v2//:execute_build.bzl", "execute_build")

filegroup(
    name = "sources",
    srcs = glob(["src/**/*"]),
    visibility = ["//visibility:public"]
)


#
# API 2.0
#

declare_module(
    name = "module",
    pom_file = ":pom.xml",
    parent = "//tests/e2e/mvn-lib-parent:module",
    visibility = ["//visibility:public"]
)

execute_build(
    name = "mvn-build-lib-one",
    pom_def = ":module",
    srcs = [":sources"],
    visibility = ["//visibility:public"]
)
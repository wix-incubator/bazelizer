load("//maven:defs.bzl", "declare_module")

load("@maven_e2e_v2//:execute_build.bzl", "execute_build")

declare_module(
    name = "module",
    pom_file = ":pom.xml",
    parent = "//tests/e2e/mvn-lib-G:module",
    visibility = ["//visibility:public"],
)

filegroup(
    name = "sources",
    srcs = glob(["src/**/*"])
)

execute_build(
    name = "mvn-lib-G-b",
    pom_def = ":module",
    srcs = [":sources"],
    visibility = ["//visibility:public"]
)
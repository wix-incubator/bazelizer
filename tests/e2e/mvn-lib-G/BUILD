load("//maven:defs.bzl", "declare_module")

load("@maven_e2e_v2//:execute_build.bzl", "execute_build")



declare_module(
    name = "module",
    pom_file = ":pom.xml",
    parent = "//tests/e2e/mvn-lib-parent:module",
    visibility = ["//visibility:public"],
)


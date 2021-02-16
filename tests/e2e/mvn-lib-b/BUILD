load("//maven:defs.bzl", "declare_module")
load("@maven_e2e_v2//:execute_build.bzl", "execute_build")


filegroup(
    name = "sources",
    srcs = glob(["src/**/*"]),
    visibility = ["//visibility:public"]
)

declare_module(
    name = "module",
    pom_file = ":pom.xml",
    visibility = ["//visibility:public"],
    mvn_flags = [ "-P my_profile" ]
)

execute_build(
    name = "mvn-lib-b",
    pom_def = ":module",
    srcs = [":sources"],
    visibility = ["//visibility:public"]
)
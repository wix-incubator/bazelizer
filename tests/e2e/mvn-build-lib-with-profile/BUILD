load("//maven:defs.bzl", "declare_pom")
load("@maven_e2e//:execute_build.bzl", "execute_build")


filegroup(
    name = "sources",
    srcs = glob(["src/**/*"]),
    visibility = ["//visibility:public"]
)

declare_pom(
    name = "declared_pom",
    pom_file = ":pom.xml",
    visibility = ["//visibility:public"],
    mvn_flags = [ "-P my_profile" ]
)

execute_build(
    name = "mvn-build-lib-with-profile",
    pom_def = ":declared_pom",
    srcs = [":sources"],
    visibility = ["//visibility:public"]
)
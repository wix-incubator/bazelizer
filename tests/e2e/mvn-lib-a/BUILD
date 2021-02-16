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
    parent = "//tests/e2e/mvn-lib-parent:module",
    visibility = ["//visibility:public"]
)

execute_build(
    name = "mvn-lib-a",
    pom_def = ":module",
    srcs = [":sources"],
    visibility = ["//visibility:public"],
    deps = [
        "//tests/e2e/mvn-build-lib-one", # maven bazed build
        "//tests/e2e/lib/src/com/mavenizer/examples/subliby", # bazel build
    ],
    outputs = ["a-1.0.0-SNAPSHOT.jar"],
)

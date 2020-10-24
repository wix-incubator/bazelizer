load("//maven:defs.bzl", "declare_pom")

load("@maven_e2e//:execute_build.bzl", "execute_build")

filegroup(
    name = "sources",
    srcs = glob(["**/*"]),
    visibility = ["//visibility:public"]
)


declare_pom(
    name = "declared_pom",
    pom_file = ":pom.xml",
    parent = "//tests/e2e/mvn-parent-pom:declared_pom",
    visibility = ["//visibility:public"]
)

execute_build(
    name = "mvn-build-lib",
    pom_def = ":declared_pom",
    srcs = [":sources"],
    visibility = ["//visibility:public"],
    deps = [
         "//tests/e2e/mvn-build-lib-one",
        "//tests/e2e/lib/src/com/mavenizer/examples/subliby",
    ],
    outputs = ["myapi-1.0.0-SNAPSHOT.jar"],
)

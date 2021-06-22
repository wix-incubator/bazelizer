load("//private/ruls/maven_v3:maven_project.bzl", "maven_project")
load("@maven_e2e_v3//:execute_build.bzl", "execute_build")

filegroup(
    name = "sources",
    srcs = glob(["src/**/*"]),
    visibility = ["//visibility:public"]
)

maven_project(
    name = "maven",
    pom_file = ":pom.xml",
    parent_project = "//tests/e2e/mvn-lib-parent:maven",
    visibility = ["//visibility:public"]
)

execute_build(
    name = "mvn-lib-a",
    project = ":maven",
    srcs = [":sources"],
    visibility = ["//visibility:public"],
    deps = [
#        "//tests/e2e/mvn-build-lib-one", # maven bazed build
        "//tests/e2e/bzl-lib/src/com/mavenizer/examples/util", # bazel build
    ],
#    outputs = ["a-1.0.0-SNAPSHOT.jar"],
)

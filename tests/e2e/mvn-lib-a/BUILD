load("//private/ruls/maven_v3:maven_project.bzl", "maven_project")

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

#execute_build(
#    name = "mvn-lib-a",
#    pom_def = ":module",
#    srcs = [":sources"],
#    visibility = ["//visibility:public"],
#    deps = [
#        "//tests/e2e/mvn-build-lib-one", # maven bazed build
#        "//tests/e2e/lib/src/com/mavenizer/examples/subliby", # bazel build
#    ],
#    outputs = ["a-1.0.0-SNAPSHOT.jar"],
#)

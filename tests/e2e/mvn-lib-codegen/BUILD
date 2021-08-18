load("//private/ruls/maven_v3:maven_project.bzl", "maven_project")
load("@maven_e2e_v3//:execute_build.bzl", "execute_build")

maven_project(
    name = "maven",
    pom_file = ":pom.xml",
    parent_project = "//tests/e2e/mvn-lib-parent:maven",
    visibility = ["//visibility:public"]
)

#execute_build(
#    name = "xsd",
#    project = ":maven",
#    srcs = glob(["src/main/xsd/*"]),
#    visibility = ["//visibility:public"],
#)

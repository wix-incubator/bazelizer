load("//private/ruls/maven_v3:maven_project.bzl", "maven_project")

filegroup(
    name = "sources",
    srcs = glob(["src/**/*"]),
    visibility = ["//visibility:public"]
)

maven_project(
    name = "maven",
    pom_file = ":pom.xml",
    visibility = ["//visibility:public"]
)
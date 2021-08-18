load("//private/ruls/maven_v3:maven_project.bzl", "maven_project")
load("@maven_e2e_v3//:execute_build.bzl", "execute_build")

maven_project(
    name = "maven",
    pom_file = ":pom.xml",
    parent_project = "//tests/e2e/mvn-lib-parent:maven",
    visibility = ["//visibility:public"],
    flags = [
        "--mvn-extra-args", "de.qaware.maven:go-offline-maven-plugin:1.2.8:resolve-dependencies"
    ]
)

#
# Go wild!
#

execute_build(
    name = "xsd",
    project = ":maven",
    srcs = glob(["src/main/xsd/schema1.xsd"]),
    visibility = ["//visibility:public"],
    flags = [
        '--mvn-override-artifact-id', 'xxxx'
    ],
)


execute_build(
    name = "mvn-lib-codegen",
    project = ":maven",
    srcs = glob(["src/main/xsd/schema2.xsd"]),
    deps = [ ":xsd", "//tests/e2e/mvn-lib-codegen/src/main/java/com/bazelizer" ],
    visibility = ["//visibility:public"],
)
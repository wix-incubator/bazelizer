load("//maven:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")

load("//private/ruls/maven:repository.bzl", "declare_pom")
load("@maven_def//:execute_build.bzl", "execute_build")


exports_files(["pom.xml"])


alias(
    name = "mvn-build-lib-one",
    actual = ":lib",
)

filegroup(
    name = "sources",
    srcs = glob(["src/**/*"]),
    visibility = ["//visibility:public"]
)

create_mvn_buildpack(
    name = "buildpack",
    pom_file = "pom.xml",
    visibility = ["//visibility:public"],
)

run_mvn_buildpack(
    name = "lib",
    log_level = "OFF",
    srcs = [":sources"],
    buildpack = ":buildpack",
    visibility = ["//visibility:public"]
)



#
# API 2.0
#

declare_pom(
    name = "declared_pom",
    pom_file = ":pom.xml",
    parent = "//tests/e2e/mvn-parent-pom:declared_pom",
    visibility = ["//visibility:public"]
)


execute_build(
    name = "mvn-build-lib-one-v2",
    pom_def = ":declared_pom",
    # log_level = "OFF",
    srcs = [":sources"],
    visibility = ["//visibility:public"]
)